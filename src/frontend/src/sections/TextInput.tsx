import {Accessor, Component, createSignal, Setter} from "solid-js";
import promptService from "../services/PromptService";
import {createPrompt, Prompt} from "../interfaces/Prompt";
import {Chat} from "../interfaces/Chat";
import chatService from "../services/ChatService";
import {useAppContext} from "../Context";
import {AuthorType} from "../interfaces/AuthorType";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";
import {LLMResponse, LLMResponseStatus} from "../interfaces/LLMResponse";

type PromptAccessorSetter = { accessor: Accessor<Prompt[]>; setter: Setter<Prompt[]> };

function processLLMResponseStatus(llmResponse: LLMResponse, eventSource: EventSource, setSendDisabled: Setter<boolean>): any {
    let content = "";
    let prompt = llmResponse.prompt;

    if(prompt != null && prompt?.authorType === AuthorType.SYSTEM) {
        prompt.temporary = llmResponse.status === LLMResponseStatus.GENERATING.toString();
    }

    switch (llmResponse.status) {
        case "SUCCESS":
            setSendDisabled(false);
            eventSource.close();
            break;
        case "ERROR":
            setSendDisabled(false);
            eventSource.close();
            Utils.showToast(`Error`, "Internal server error occurred", "danger", "bi-exclamation-triangle");
            break;
        case "TIMEOUT":
            setSendDisabled(false);
            eventSource.close();
            content = llmResponse.content!;
            Utils.showToast(`Error`, "Timeout error occurred", "danger", "bi-exclamation-triangle");
            break;
        case "GENERATING":
            setSendDisabled(true);
            content = llmResponse.content!;
            break;
        case "FINISH" :
            setSendDisabled(false);
            content = "Closing communication with server";
            prompt = null;
            eventSource.close();
            break;
    }

    return { status: llmResponse.status, content, prompt };
}

function handleSystemAuthorType(curChatPrompts: PromptAccessorSetter, systemPrompt: Prompt, content: string, indexOfPrompt: number): number {
    let newIndex = indexOfPrompt;
    if(indexOfPrompt >= 0) {
        let prompt = curChatPrompts.accessor()[indexOfPrompt];
        prompt = { ...prompt, message: content };
        curChatPrompts.setter(prev => prev.map((p, i) => i === indexOfPrompt ? prompt : p));
    } else {
        newIndex = curChatPrompts.accessor().length;
        curChatPrompts.setter(prev => [...prev, { ...systemPrompt, message: content }]);
    }
    return newIndex;
}

function handleLLMResponse(
    llmResponse: LLMResponse,
    eventSource: EventSource,
    curChatPrompts: PromptAccessorSetter,
    setSendDisabled: Setter<boolean>,
    systemPrompt: Prompt,
    indexOfPrompt: number = -1
): number {
    const { status, content, prompt } = processLLMResponseStatus(llmResponse, eventSource, setSendDisabled);

    if (status === LLMResponseStatus.GENERATING.toString()) {
        return handleSystemAuthorType(curChatPrompts, systemPrompt, content, indexOfPrompt);
    } else if(status === LLMResponseStatus.SUCCESS.toString() || status === LLMResponseStatus.ERROR.toString()) {
        curChatPrompts.setter(prev => prev.filter((_, i) => i !== indexOfPrompt));
        curChatPrompts.setter(prev => [...prev, prompt]);
    }
    return indexOfPrompt;
}

const TextInput: Component = () => {
    const [{curChatId, selectedLLM, curChatPrompts, chats}] = useAppContext();
    const [message, setMessage] = createSignal("");
    const [sendDisabled, setSendDisabled] = createSignal(false);

    const fetchLLMResponse = async () => {
        const messageToSend = message();
        setMessage("");
        try {
            const newPrompt: Prompt = createPrompt(messageToSend, AuthorType.USER, curChatId.accessor()!);
            await insertNewPrompt(newPrompt);
            await generatorService.generateResponseFromLLM(newPrompt, (llmResponse: LLMResponse, eventSource: EventSource, systemPrompt: Prompt, IndexOfPrompt: number) => {
                return handleLLMResponse(llmResponse, eventSource, curChatPrompts, setSendDisabled, systemPrompt, IndexOfPrompt);
            });
        } catch (error) {
            console.error("Error fetching llm response:", error);
        }
    }

    const handleSend = async () => {
        if (!message().trim()) return;

        if (!curChatId.accessor() && !selectedLLM.accessor()) {
            Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
        } else {
            setSendDisabled(true);
            if (!curChatId.accessor() && selectedLLM.accessor()) {
                await createNewChat(message());
            }
            await fetchLLMResponse();
            setSendDisabled(false);
        }
    };

    const createNewChat = async (title = "") => {
        if(selectedLLM.accessor()) {
            const newChat: Chat = { id: 0, title: title, lastActivity: Date.now(), llmId: selectedLLM.accessor()!.id };
            try {
                const createdChat = await chatService.createChat(newChat);
                curChatId.setter(createdChat.id);
                chats.refetcher();
            } catch (error) {
                console.error("Error creating chat:", error);
            }
        } else {
            Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
        }
    };

    const insertNewPrompt = async (prompt: Prompt, send: boolean = false) => {
        try {
            if(send) {
                await promptService.createPrompt(prompt);
            }
            curChatPrompts.setter(prev => [...prev, prompt]);
        } catch (error) {
            console.error("Failed to create prompt:", error);
        }
    }

    return (
        <div class="input-group d-flex justify-content-center">
            <div class="input-group w-50 mb-2 d-flex">
              <textarea
                  class="form-control rounded-start"
                  rows="1"
                  placeholder="Type your message here..."
                  style="resize: none;"
                  value={message()}
                  onInput={(e) => setMessage(e.currentTarget.value)}
                  onKeyPress={(e) => {
                      if (e.key === 'Enter' && !sendDisabled()) {
                          e.preventDefault();
                          handleSend();
                      }
                  }}
              ></textarea>
                <button class="btn btn-primary rounded-end" type="button" disabled={sendDisabled()} onClick={handleSend}>
                    <i class="bi bi-send-fill"></i>
                </button>
            </div>
        </div>
    );
};

export default TextInput;

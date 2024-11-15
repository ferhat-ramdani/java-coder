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
import {LLM} from "../interfaces/LLM";

type PromptAccessorSetter = { accessor: Accessor<Prompt[]>; setter: Setter<Prompt[]> };

function processLLMResponseStatus(
    llmResponse: LLMResponse,
    eventSource: EventSource,
    setSendDisabled: Setter<boolean>
): any {
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

function handleSystemAuthorType(
    curChatPrompts: PromptAccessorSetter,
    systemPrompt: Prompt,
    content: string,
    indexOfPrompt: number
): number {
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

const insertNewPrompt = async (
    prompt: Prompt,
    curChatPrompts : PromptAccessorSetter,
    send: boolean = false
) => {
    try {
        if(send) {
            await promptService.createPrompt(prompt);
        }
        curChatPrompts.setter(prev => [...prev, prompt]);
    } catch (error) {
        console.error("Failed to create prompt:", error);
    }
}

const createNewChat = async (
    title: string,
    selectedLLM: LLM | null,
    curChatId: Setter<number | null>,
    chats: { refetcher: () => void }
) => {
    if (selectedLLM) {
        const newChat: Chat = { id: 0, title: title, lastActivity: Date.now(), llmId: selectedLLM!.id };
        try {
            const createdChat = await chatService.createChat(newChat);
            curChatId(createdChat.id);
            chats.refetcher();
        } catch (error) {
            console.error("Error creating chat:", error);
        }
    } else {
        Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
    }
};

const fetchLLMResponse = async (
    message: string,
    setMessage: Setter<string>,
    curChatId: Accessor<number | null>,
    curChatPrompts: PromptAccessorSetter,
    setSendDisabled: Setter<boolean>
) => {
    const messageToSend = message;
    setMessage("");
    try {
        const newPrompt: Prompt = createPrompt(messageToSend, AuthorType.USER, curChatId()!);
        await insertNewPrompt(newPrompt, curChatPrompts);
        await generatorService.generateResponseFromLLM(newPrompt,
            (llmResponse: LLMResponse, eventSource: EventSource, systemPrompt: Prompt, IndexOfPrompt: number) => {
            return handleLLMResponse(llmResponse, eventSource, curChatPrompts, setSendDisabled, systemPrompt, IndexOfPrompt);
        });
    } catch (error) {
        console.error("Error fetching llm response:", error);
    }
};

const handleSendMessage = async (
    message: Accessor<string>,
    setMessage: Setter<string>,
    curChatId: { accessor : Accessor<number|null>, setter : Setter<number | null> },
    selectedLLM: Accessor<LLM | null>,
    curChatPrompts: PromptAccessorSetter,
    setSendDisabled: Setter<boolean>,
    chats: { refetcher: () => void }
) => {
    if (!message().trim()) return;

    if (!curChatId.accessor() && !selectedLLM()) {
        Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
    } else {
        setSendDisabled(true);
        if (!curChatId.accessor() && selectedLLM()) {
            await createNewChat(message(), selectedLLM(), curChatId.setter, chats);
        }
        await fetchLLMResponse(message(), setMessage, curChatId.accessor, curChatPrompts, setSendDisabled);
        setSendDisabled(false);
    }
};

const TextInput: Component = () => {
    const [{curChatId, selectedLLM, curChatPrompts, chats}] = useAppContext();
    const [message, setMessage] = createSignal("");
    const [sendDisabled, setSendDisabled] = createSignal(false);


    const handleSend = async () => {
        await handleSendMessage(message, setMessage, curChatId, selectedLLM.accessor, curChatPrompts, setSendDisabled, chats);
    };

    return (
        <div class="input-group d-flex justify-content-center">
            <div class="input-group w-100 d-flex">
              <textarea
                  class="form-control rounded-start"
                  rows="2"
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

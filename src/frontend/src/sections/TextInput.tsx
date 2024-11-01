import {Accessor, Component, createSignal, Resource, Setter} from "solid-js";
import promptService from "../services/PromptService";
import {createPrompt, Prompt} from "../interfaces/Prompt";
import {Chat} from "../interfaces/Chat";
import chatService from "../services/ChatService";
import {useAppContext} from "../Context";
import {AuthorType} from "../interfaces/AuthorType";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";
import {LLMResponse} from "../interfaces/LLMResponse";

type PromptAccessorSetter = { accessor: Accessor<Prompt[]>; setter: Setter<Prompt[]> };
type ChatIdAccessorSetter = { accessor: Accessor<number | null>; setter: Setter<number | null> };

function processLLMResponseStatus(llmResponse: LLMResponse, eventSource: EventSource): any {
    let content = "";
    let author = AuthorType.SYSTEM;
    let prompt = llmResponse.prompt;

    switch (llmResponse.status) {
        case "DONE":
            author = AuthorType.AI;
            eventSource.close();
            break;
        case "ERROR":
        case "TIMEOUT":
            eventSource.close();
            content = llmResponse.content!;
            break;
        case "GENERATING":
            content = llmResponse.content!;
            break;
    }

    return { content, author, prompt };
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
    systemPrompt: Prompt,
    curChatId: ChatIdAccessorSetter,
    indexOfPrompt: number = -1
): number {
    const { content, author, prompt } = processLLMResponseStatus(llmResponse, eventSource);

    if (author === AuthorType.SYSTEM) {
        return handleSystemAuthorType(curChatPrompts, systemPrompt, content, indexOfPrompt);
    } else {
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
            insertNewPrompt(newPrompt);
            await generatorService.generateResponseFromLLM(newPrompt, (llmResponse: LLMResponse, eventSource: EventSource, systemPrompt: Prompt, IndexOfPrompt: number) => {
                return handleLLMResponse(llmResponse, eventSource, curChatPrompts, systemPrompt, curChatId, IndexOfPrompt);
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

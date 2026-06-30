import {Accessor, Component, createSignal, Setter} from "solid-js";
import promptService from "../services/PromptService";
import {createPrompt, Prompt} from "../interfaces/Prompt";
import {AuthorType} from "../interfaces/AuthorType";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";
import {LLMResponse, LLMResponseStatus} from "../interfaces/LLMResponse";

type PromptAccessorSetter = { accessor: Accessor<Prompt[]>; setter: Setter<Prompt[]> };

interface TextInputProps {
    chatId: number,
    prompts: PromptAccessorSetter,
}

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
            Utils.showToast(`Error`, prompt?.message || "Internal server error occurred", "danger", "bi-exclamation-triangle", 4000);
            break;
        case "GENERATING":
            setSendDisabled(true);
            content = llmResponse.content!;
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

const fetchLLMResponse = async (
    message: string,
    setMessage: Setter<string>,
    curChatId: number,
    curChatPrompts: PromptAccessorSetter,
    setSendDisabled: Setter<boolean>
) => {
    const messageToSend = message;
    setMessage("");
    try {
        const newPrompt: Prompt = createPrompt(messageToSend, AuthorType.USER, curChatId);
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
    curChatId: number,
    curChatPrompts: PromptAccessorSetter,
    setSendDisabled: Setter<boolean>,
) => {
    if (!message().trim()) return;

    if (!curChatId && !localStorage.getItem('default-llm')) {
        Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
    } else {
        setSendDisabled(true);
        await fetchLLMResponse(message(), setMessage, curChatId, curChatPrompts, setSendDisabled);
        setSendDisabled(false);
    }
};

const TextInput: Component<TextInputProps> = (props) => {
    const [message, setMessage] = createSignal("");
    const [sendDisabled, setSendDisabled] = createSignal(false);


    const handleSend = async () => {
        await handleSendMessage(message, setMessage, props.chatId, props.prompts, setSendDisabled);
    };

    return (
        <div class="input-group d-flex justify-content-center mb-2">
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

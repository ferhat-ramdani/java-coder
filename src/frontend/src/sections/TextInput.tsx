import {Accessor, Component, createSignal, Setter} from "solid-js";
import promptService from "../services/PromptService";
import {createPrompt, Prompt} from "../interfaces/Prompt";
import {AuthorType} from "../interfaces/AuthorType";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";
import {GenerationStep, LLMResponse, LLMResponseStatus} from "../interfaces/LLMResponse";

type PromptAccessorSetter = { accessor: Accessor<Prompt[]>; setter: Setter<Prompt[]> };

interface TextInputProps {
    chatId: number,
    prompts: PromptAccessorSetter,
}

function appendProgressStep(
    curChatPrompts: PromptAccessorSetter,
    llmResponse: LLMResponse,
    chatId: number,
    indexOfPrompt: number
): number {
    const step: GenerationStep = {
        phase: llmResponse.phase,
        attempt: llmResponse.attempt,
        maxAttempts: llmResponse.maxAttempts,
        message: llmResponse.message ?? "",
        detail: llmResponse.detail ?? null,
        timestamp: Date.now(),
    };

    if (indexOfPrompt >= 0) {
        curChatPrompts.setter(prev => prev.map((p, i) =>
            i === indexOfPrompt ? {...p, generationHistory: [...(p.generationHistory ?? []), step]} : p));
        return indexOfPrompt;
    }

    const systemPrompt: Prompt = {...createPrompt("", AuthorType.SYSTEM, chatId, false, true), generationHistory: [step]};
    let newIndex = -1;
    curChatPrompts.setter(prev => {
        newIndex = prev.length;
        return [...prev, systemPrompt];
    });
    return newIndex;
}

function handleLLMResponse(
    llmResponse: LLMResponse,
    eventSource: EventSource,
    curChatPrompts: PromptAccessorSetter,
    setSendDisabled: Setter<boolean>,
    chatId: number,
    indexOfPrompt: number
): number {
    switch (llmResponse.status) {
        case LLMResponseStatus.PROGRESS:
            setSendDisabled(true);
            return appendProgressStep(curChatPrompts, llmResponse, chatId, indexOfPrompt);
        case LLMResponseStatus.SUCCESS:
        case LLMResponseStatus.ERROR: {
            setSendDisabled(false);
            eventSource.close();
            if (llmResponse.status === LLMResponseStatus.ERROR) {
                Utils.showToast("Error", llmResponse.prompt?.message || "Internal server error occurred", "danger", "bi-exclamation-triangle", 4000);
            }
            const history = indexOfPrompt >= 0 ? curChatPrompts.accessor()[indexOfPrompt]?.generationHistory : undefined;
            curChatPrompts.setter(prev => indexOfPrompt >= 0 ? prev.filter((_, i) => i !== indexOfPrompt) : prev);
            if (llmResponse.prompt) {
                curChatPrompts.setter(prev => [...prev, {...llmResponse.prompt!, generationHistory: history}]);
            }
            return -1;
        }
    }
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
        let index = -1;
        await generatorService.generateResponseFromLLM(newPrompt, (llmResponse: LLMResponse, eventSource: EventSource) => {
            index = handleLLMResponse(llmResponse, eventSource, curChatPrompts, setSendDisabled, curChatId, index);
        });
    } catch (error) {
        console.error("Error fetching llm response:", error);
        setSendDisabled(false);
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
    }
};

const MAX_TEXTAREA_ROWS = 8;

const TextInput: Component<TextInputProps> = (props) => {
    const [message, setMessage] = createSignal("");
    const [sendDisabled, setSendDisabled] = createSignal(false);

    let textareaRef: HTMLTextAreaElement | undefined;

    const autoGrow = () => {
        if (!textareaRef) return;
        textareaRef.style.height = "auto";
        const lineHeight = parseFloat(getComputedStyle(textareaRef).lineHeight || "20");
        const maxHeight = lineHeight * MAX_TEXTAREA_ROWS;
        textareaRef.style.height = `${Math.min(textareaRef.scrollHeight, maxHeight)}px`;
    };

    const handleSend = async () => {
        await handleSendMessage(message, setMessage, props.chatId, props.prompts, setSendDisabled);
        requestAnimationFrame(autoGrow);
    };

    const canSend = () => !sendDisabled() && message().trim().length > 0;

    return (
        <div class="composer-bar chat-max-width mb-3">
            <div class="composer">
                <textarea
                    ref={textareaRef}
                    class="composer-input"
                    rows="1"
                    placeholder="Message Java Coder..."
                    value={message()}
                    onInput={(e) => {
                        setMessage(e.currentTarget.value);
                        autoGrow();
                    }}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                            e.preventDefault();
                            if (canSend()) handleSend();
                        }
                    }}
                ></textarea>
                <button
                    class={`composer-send-btn ${canSend() ? 'active' : ''}`}
                    type="button"
                    disabled={!canSend()}
                    title="Send message"
                    onClick={handleSend}
                >
                    <i class={sendDisabled() ? "bi bi-hourglass-split" : "bi bi-arrow-up-short"}></i>
                </button>
            </div>
        </div>
    );
};

export default TextInput;

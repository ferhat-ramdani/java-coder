import {useNavigate, useParams} from "@solidjs/router";
import {Component, createSignal, For, onMount} from "solid-js";
import TextInput from "./TextInput";
import PromptMessage from "./PromptMessage";
import {Prompt} from "../interfaces/Prompt";
import {useAppContext} from "../Context";
import chatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import llmService from "../services/LLMService";
import {Utils} from "../services/Utils";


const ChatUI: Component = () => {
    const params = useParams();

    const navigate = useNavigate();
    const [curChatPrompts, setCurChatPrompts] = createSignal<Prompt[]>([]);
    const [{pageTitle}] = useAppContext();

    const chatPrompts = { accessor: curChatPrompts, setter: setCurChatPrompts };

    let containerRef: HTMLDivElement | undefined;

    const scrollToBottom = () => {
        if (containerRef) {
            containerRef.scrollTop = containerRef.scrollHeight;
        }
    };

    onMount(async () => {
        try {
            const chat = await chatService.getChatById(+params.id);
            const prompts = await PromptService.getPromptsByChatId(chat.id);
            const llm = await llmService.getLlmById(chat.llmId);
            chatPrompts.setter(prompts);
            pageTitle.setter(`LLM: ${llm.name}`);

            scrollToBottom();
        } catch (error) {
            Utils.showToast("Error", "Failed to fetch chat", "danger", "bi-exclamation-triangle");
            navigate("/chats");
        }
    });


    return (<>
            <div class={`container-fluid d-flex flex-column flex-grow-1 overflow-hidden`}>
                <div ref={containerRef} class="flex-grow-1 overflow-auto p-3 border rounded my-3">
                    <For each={chatPrompts.accessor()} fallback={`Prompt Example`}>
                        {(prompt) => <PromptMessage prompt={prompt}/>}
                    </For>
                </div>
                <TextInput chatId={+params.id} prompts={chatPrompts}/>
            </div>
        </>);
};

export default ChatUI;



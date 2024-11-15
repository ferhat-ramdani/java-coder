import {Component, createSignal, For, onMount} from "solid-js";
import PromptMessage from "./PromptMessage";
import PromptService from "../services/PromptService";
import {Prompt} from "../interfaces/Prompt";
import chatService from "../services/ChatService";
import {Utils} from "../services/Utils";
import {useNavigate} from "@solidjs/router";

interface PromptsContainerProps {
    chatId: number
}
const PromptsContainer: Component<PromptsContainerProps> = (props: PromptsContainerProps) => {

    const navigate = useNavigate();
    const [curChatPrompts, setCurChatPrompts] = createSignal<Prompt[]>([]);

    onMount(async () => {
        if(props.chatId === 0) {

        } else {
            try {
                const chat = await chatService.getChatById(props.chatId);
                const prompts = await PromptService.getPromptsByChatId(chat.id);
                setCurChatPrompts(prompts);
            } catch (error) {
                Utils.showToast("Error", "Failed to fetch chat", "danger", "bi-exclamation-triangle");
                navigate("/chats");
            }
        }
    });

    return (
            <For each={curChatPrompts()} fallback={`Prompt Example`}>
                {(prompt) => <PromptMessage prompt={prompt}/>}
            </For>
            );
};

export default PromptsContainer;

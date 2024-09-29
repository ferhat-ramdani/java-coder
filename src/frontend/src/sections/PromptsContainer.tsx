import {Component, createSignal, createEffect, Setter} from "solid-js";
import PromptMessage from "./PromptMessage";
import promptService from "../services/PromptService";
import { Prompt } from "../interfaces/Prompt";

interface PromptContainerProps {
    curChatId: () => number | null;
    refreshPrompts: () => boolean;
    setRefreshPrompts: Setter<boolean>;
}

const PromptsContainer: Component<PromptContainerProps> = (props) => {
    const [prompts, setPrompts] = createSignal<Prompt[]>([]);

    createEffect(async () => {
        const chatId = props.curChatId();
        if (chatId) {
            if (props.refreshPrompts() || !props.refreshPrompts()) {
                try {
                    const chatPrompts = await promptService.getPromptsByChatId(chatId);
                    setPrompts(chatPrompts);
                    console.log(chatPrompts);
                } catch (error) {
                    console.error("Failed to fetch prompts", error);
                }
            }
            props.setRefreshPrompts(false);
        } else {
            setPrompts([]);
        }
    });

    return (
        <div class="prompt-container w-50 flex-grow-1 overflow-auto">
            {prompts().map((prompt) => (
                <PromptMessage
                    type={prompt.authorType.toLowerCase() === "user" ? "user" : "llm"}
                    message={prompt.authorType.toLowerCase() === "user" ? prompt.message : prompt.llmResponse}
                />
            ))}
        </div>
    );
};

export default PromptsContainer;

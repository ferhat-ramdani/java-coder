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
        <div class="flex-grow-1 p-0 container-fluid h-100 w-100 overflow-auto">
            <div class="row w-100">
                <div class="col-2"></div>
                <div class="col-8">
                    {prompts().map((prompt) => (
                        <PromptMessage
                            type={prompt.authorType.toLowerCase() === "user" ? "user" : "llm"}
                            message={prompt.message}
                        />
                    ))}
                </div>
            </div>
        </div>
    );
};

export default PromptsContainer;

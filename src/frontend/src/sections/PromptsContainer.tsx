import { Component, onMount, createSignal } from "solid-js";
import PromptMessage from "./PromptMessage";
import promptService from "../services/PromptService";
import {Prompt} from "../interfaces/Prompt"

const PromptsContainer: Component = () => {
    const [prompts, setPrompts] = createSignal<Prompt[]>([]);

    onMount(async () => {
        try {
            const chatPrompts = await promptService.getPromptsByChatId(1);
            console.log(chatPrompts);
            setPrompts(chatPrompts);
        } catch (error) {
            console.error("Failed to fetch prompts", error);
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

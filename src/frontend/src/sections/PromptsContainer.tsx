import {Component} from "solid-js";
import PromptMessage from "./PromptMessage";
import {useAppContext} from "../Context";

const PromptsContainer: Component = () => {
    const [{curChatPrompts}] = useAppContext();

    return (
        <div class="flex-grow-1 p-0 container-fluid h-100 w-100 overflow-auto">
            <div class="row w-100">
                <div class="col-2"></div>
                <div class="col-8">
                    {curChatPrompts.accessor().map((prompt) => (
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

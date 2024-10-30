import {Component, For, onMount} from "solid-js";
import PromptMessage from "./PromptMessage";
import PromptService from "../services/PromptService";
import {useAppContext} from "../Context";

const PromptsContainer: Component = () => {
    onMount(() => {
        console.log("starting progressive test");
        PromptService.testProgressive();
        console.log("starting progressive finished");
    });

    const [{curChatPrompts}] = useAppContext();

    return (<div class="flex-grow-1 p-0 container-fluid h-100 w-100 overflow-auto">
            <div class="row w-100">
                <div class="col-2"></div>
                <div class="col-8">
                    <For each={curChatPrompts.accessor()} fallback={`Prompt Example`}>
                        {(prompt) => <PromptMessage prompt={prompt}/>}
                    </For>
                </div>
            </div>
        </div>);
};

export default PromptsContainer;

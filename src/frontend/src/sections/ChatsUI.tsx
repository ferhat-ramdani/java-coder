import {Component, createResource, For, onMount, Show} from "solid-js";
import ChatItem from "./ChatItem";
import {useAppContext} from "../Context";
import {SpinnerSmall} from "./Spinner";
import llmService from "../services/LLMService";
import {LLM} from "../interfaces/LLM";

const fetchFirstLLM = async (): Promise<LLM|null> => {
    try {
        return await llmService.getFirstLLM();
    } catch (error) {
        console.error("Error fetching first llm:", error);
        return null;
    }
}

const ChatsUI: Component = () => {

    onMount(async () => {
        const llm = await fetchFirstLLM();
        selectedLLM.setter(llm);
    });

    const [{selectedLLM, chats}] = useAppContext();

    const createNewChat = async () => {
        selectedLLM.setter(null);
        const llm = await llmService.getFirstLLM();
        selectedLLM.setter(llm);
    };

    return (
        <div class="container mt-3">
            <div class="d-flex justify-content-between align-items-center align-content-center mb-1">
                <h2 class={`m-0`}>Chat Information</h2>
                <button type="button" class="btn btn-success btn-sm" onClick={createNewChat}><i class="bi bi-plus-lg fs-5"></i></button>
            </div>
            <ul class="list-group">
                <Show when={!chats.resource.loading} fallback={<SpinnerSmall text={`Loading chats`}/>}>
                    <For each={chats.resource()} fallback={'No Chats'}>
                        {chat => <ChatItem chat={chat}/>}
                    </For>
                </Show>
            </ul>
        </div>
    );
};

export default ChatsUI;

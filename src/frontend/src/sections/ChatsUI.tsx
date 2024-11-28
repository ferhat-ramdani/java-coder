import {Component, createSignal, For, onMount, Show} from "solid-js";
import ChatItem from "./ChatItem";
import {useAppContext} from "../Context";
import {Spinner, SpinnerSmall} from "./Spinner";
import llmService from "../services/LLMService";
import {LLM} from "../interfaces/LLM";
import chatService from "../services/ChatService";
import {Chat} from "../interfaces/Chat";
import {useNavigate} from "@solidjs/router";

const fetchFirstLLM = async (): Promise<LLM|null> => {
    try {
        return await llmService.getFirstLLM();
    } catch (error) {
        console.error("Error fetching first llm:", error);
        return null;
    }
}

const ChatsUI: Component = () => {
    const [{chats, pageTitle}] = useAppContext();
    const navigate = useNavigate();
    const [selectedLLM, setSelectedLLM] = createSignal<LLM | null>(null);

    onMount(async () => {
    let storedLLMId = localStorage.getItem('default-llm');
    if (storedLLMId === null) {
        const llm = await fetchFirstLLM();
        storedLLMId = llm!.id.toString();
        localStorage.setItem('default-llm', storedLLMId);
        setSelectedLLM(llm);
    } else {
        const llm = await llmService.getLlmById(JSON.parse(storedLLMId));
        setSelectedLLM(llm);
    }
    pageTitle.setter("List of chats");
    chats.refetcher();
});

    const createNewChat = async () => {
        const newChat: Chat = { id: 0, title: "", lastActivity: Date.now(), llmId: selectedLLM()!.id };
        try {
            const createdChat = await chatService.createChat(newChat);
            navigate(`/chats/${createdChat.id}`);
        } catch (error) {
            console.error("Error creating chat:", error);
        }
    };


    return (
        <Show when={selectedLLM()} fallback={<Spinner text={`Loading Chats`}/>}>
            <div class="container mt-3">
                <div class="d-flex justify-content-between align-items-center align-content-center mb-1">
                    <h2 class={`m-0`}>Current llm: {selectedLLM()?.name}</h2>
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
        </Show>
    );
};

export default ChatsUI;

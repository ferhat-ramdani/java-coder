import {Component, createSignal, For, onMount, Show} from "solid-js";
import ChatItem from "./ChatItem";
import {useAppContext} from "../Context";
import {Spinner, SpinnerSmall} from "./Spinner";
import llmService from "../services/LLMService";
import {LLM} from "../interfaces/LLM";
import chatService from "../services/ChatService";
import {Chat} from "../interfaces/Chat";
import {A, useNavigate} from "@solidjs/router";

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
            <div class="chat-max-width fade-in mt-4">
                <div class="d-flex justify-content-between align-items-center mb-4 px-3">
                    <h3 class={`m-0 fw-semibold text-main`}>
                        <i class="bi bi-cpu me-2"></i>Active Model: <span class="text-primary">{selectedLLM()?.name}</span>
                    </h3>
                    <button type="button" class="btn btn-primary shadow-sm hover-lift" onClick={createNewChat} disabled={!selectedLLM()?.installed}>
                        <i class="bi bi-plus-lg me-1"></i> New Chat
                    </button>
                </div>
                <Show when={!selectedLLM()?.installed}>
                    <div class="alert alert-warning d-flex align-items-center mx-3" role="alert">
                        <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
                        <span>The active model isn't downloaded yet. Go to the <A href="/llms">LLMs page</A> to download it before starting a chat.</span>
                    </div>
                </Show>
                <div class="card shadow-sm border-0">
                    <div class="card-body p-0">
                        <ul class="list-group list-group-flush rounded">
                            <Show when={!chats.resource.loading} fallback={<div class="p-4 text-center"><SpinnerSmall text={`Loading chats`}/></div>}>
                                <For each={chats.resource()} fallback={<div class="p-4 text-center text-muted">No Chats Found. Start a new one!</div>}>
                                    {chat => <ChatItem chat={chat}/>}
                                </For>
                            </Show>
                        </ul>
                    </div>
                </div>
            </div>
        </Show>
    );
};

export default ChatsUI;

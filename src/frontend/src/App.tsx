import {Component, createResource, createSignal} from 'solid-js';

import 'bootstrap/dist/css/bootstrap.min.css';
import Sidebar from "./sections/Sidebar";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import LLMModelSelector from "./sections/LLMModelSelector";
import {LLM} from "./interfaces/LLM";
import {Chat} from "./interfaces/Chat";
import chatService from "./services/ChatService";

const fetchChats = async (): Promise<Chat[]> => {
    try {
        return await chatService.getChats();
    } catch (error) {
        console.error("Error fetching chats:", error);
        return [];
    }
};

const App: Component = () => {
    const [curChatId, setCurChatId] = createSignal<number | null>(null);
    const [refreshPrompts, setRefreshPrompts] = createSignal<boolean>(false);
    const [selectedLLM, setSelectedLLM] = createSignal<LLM | null>(null);
    const [chats, { refetch }] = createResource(fetchChats);
    const props = { curChatId, refreshPrompts, setCurChatId, setRefreshPrompts, selectedLLM, setSelectedLLM, chats, refetch};

    return (
        <div class="d-flex" style="height: 100vh;">
            <div style="width: 300px; max-width: 25%; min-width: 150px;">
                <Sidebar {...props} />
            </div>
            <div class="d-flex flex-column flex-grow-1 align-items-center justify-content-between" style="flex: 1;">
                <div class="d-flex justify-content-between bg-light align-items-center"
                     style="width: 100%; padding: 5px; border-bottom: 1px solid #ccc;">
                    <span >ClassGen</span>
                    <LLMModelSelector {...props} />
                </div>
                <PromptsContainer {...props} />
                <TextInput {...props} />
            </div>
        </div>


    );
};

export default App;

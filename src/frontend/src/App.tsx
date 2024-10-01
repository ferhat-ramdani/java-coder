import {Component, createResource, createSignal} from 'solid-js';

import 'bootstrap/dist/css/bootstrap.min.css';
import Sidebar from "./sections/Sidebar";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import TopBar from "./sections/TopBar";
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
        <div class="container-fluid vh-100">
            <div class="row h-100">
                <Sidebar {...props} />
                <div class="col-10 d-flex flex-column p-0 h-100">
                    <TopBar {...props} />
                    <PromptsContainer {...props} />
                    <TextInput {...props} />
                </div>
            </div>
        </div>
    );
};

export default App;

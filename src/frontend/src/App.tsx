import { Component, createSignal } from 'solid-js';

import 'bootstrap/dist/css/bootstrap.min.css';
import Sidebar from "./sections/Sidebar";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import LLMModelSelector from "./sections/LLMModelSelector";

const App: Component = () => {
    const [curChatId, setCurChatId] = createSignal<number | null>(null);
    const [refreshPrompts, setRefreshPrompts] = createSignal<boolean>(false);

    return (
        <div class="d-flex" style="height: 100vh;">
            <Sidebar setCurChatId={setCurChatId}/>
            <LLMModelSelector />
            <div class="flex-grow-1 d-flex flex-column align-items-center justify-content-between">
                <PromptsContainer curChatId={curChatId} refreshPrompts={refreshPrompts} setRefreshPrompts={setRefreshPrompts}/>
                <TextInput curChatId={curChatId} setRefreshPrompts={setRefreshPrompts}/>
            </div>
        </div>
    );
};

export default App;

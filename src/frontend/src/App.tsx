import {Component, createResource, createSignal} from 'solid-js';

import 'bootstrap/dist/css/bootstrap.min.css';
import Sidebar from "./sections/Sidebar";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import TopBar from "./sections/TopBar";
import {LLM} from "./interfaces/LLM";
import {Chat} from "./interfaces/Chat";
import chatService from "./services/ChatService";
import {ContextProvider} from "./Context";

const App: Component = () => {

    return (
        <div class="container-fluid vh-100">
            <div class="row h-100">
                <ContextProvider>
                    <Sidebar />
                    <div class="col-10 d-flex flex-column p-0 h-100">
                        <TopBar />
                        <PromptsContainer />
                        <TextInput />
                    </div>
                </ContextProvider>
            </div>
        </div>
    );
};

export default App;

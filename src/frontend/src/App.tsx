import {Component, Suspense} from 'solid-js';

import ChatsUI from "./sections/ChatsUI";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import TopBar from "./sections/TopBar";
import {ContextProvider} from "./Context";
import {Spinner} from "./sections/Spinner";

const App: Component = () => {

    return (
        <>
            <ChatsUI/>
            <PromptsContainer/>
            <TextInput/>
        </>
    );
};

export default App;

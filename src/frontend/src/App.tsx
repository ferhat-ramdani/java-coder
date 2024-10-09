import {Component, Suspense} from 'solid-js';

import Sidebar from "./sections/Sidebar";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import TopBar from "./sections/TopBar";
import {ContextProvider} from "./Context";
import {Spinner} from "./sections/Spinner";

const App: Component = () => {

    return (
        <Suspense fallback={<Spinner text="Loading..."/>}>
            <div class="container-fluid vh-100">
                <div class="row h-100">
                    <ContextProvider>
                        <Sidebar/>
                        <div class="col-10 d-flex flex-column p-0 h-100">
                            <TopBar/>
                            <PromptsContainer/>
                            <TextInput/>
                        </div>
                    </ContextProvider>
                </div>
            </div>
        </Suspense>
    );
};

export default App;

import {useParams} from "@solidjs/router";
import {Component, onMount} from "solid-js";
import PromptsContainer from "./PromptsContainer";
import TextInput from "./TextInput";


const ChatUI: Component = () => {
    const params = useParams();

    return (<>
            <div class={`container-fluid d-flex flex-column flex-grow-1 overflow-hidden`}>
                <div class="flex-grow-1 overflow-auto p-3 border rounded my-3">
                    <PromptsContainer chatId={+params.id}/>
                </div>
                <div class="d-flex my-2 p-2 border bg-light rounded">
                    <TextInput/>
                </div>
            </div>
        </>);
};

export default ChatUI;



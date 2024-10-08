import {Component, For, Show} from "solid-js";
import ChatItem from "./ChatItem";
import {useAppContext} from "../Context";
import {SpinnerSmall} from "./Spinner";

const Sidebar: Component = () => {
    const [{curChatId, curChatPrompts, selectedLLM, chats}] = useAppContext();
    const createNewChat = () => {
        curChatId.setter(null);
        selectedLLM.setter(null);
        curChatPrompts.setter([]);
    };

    return (
        <div class="d-flex flex-column p-0 col-2 h-100 bg-light border-end">
            <div class="p-3">
                <button class="w-100 btn btn-outline-primary" onClick={createNewChat}>New Chat</button>
            </div>
            <div class="h4 ms-4 mt-3">Chat History</div>
            <div class="flex-grow-1 d-flex overflow-auto list-group flex-column p-3">
                <Show when={!chats.resource.loading} fallback={<SpinnerSmall text={`Loading chats`}/>}>
                    <For each={chats.resource()} fallback={'No Chats'}>
                        {chat => <ChatItem chat={chat} />}
                    </For>
                </Show>
            </div>
        </div>
    );
};

export default Sidebar;

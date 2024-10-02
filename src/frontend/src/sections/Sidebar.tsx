import {Component, createResource, createSignal, For, Resource} from "solid-js";
import { Chat } from "../interfaces/Chat";
import { LLM } from "../interfaces/LLM";
import ChatItem from "./ChatItem";

interface SideBarProps {
    curChatId: () => number | null;
    setCurChatId: (id: number | null) => void;
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm: LLM | null) => void;
    refetch: () => Chat[] | Promise<Chat[] | undefined> | null | undefined;
    chats: Resource<Chat[]>;
}

const Sidebar: Component<SideBarProps> = (props) => {

    const createNewChat = () => {
        props.setCurChatId(null);
        props.setSelectedLLM(null);
    };

    return (
        <div class="d-flex flex-column p-0 col-2 h-100 bg-light border-end">
            <div class="p-3">
                <button class="w-100 btn btn-outline-primary" onClick={createNewChat}>New Chat</button>
            </div>
            <div class="h4 ms-4 mt-3">Chat History</div>
            <div class="flex-grow-1 d-flex overflow-auto list-group flex-column p-3">
                {props.chats() ? (
                    <For each={props.chats()}>
                        {(chat) => (
                            <ChatItem chat={chat} {...props} />
                        )}
                    </For>
                ) : (
                    <p>Loading chats...</p>
                )}
            </div>
        </div>
    );
};

export default Sidebar;

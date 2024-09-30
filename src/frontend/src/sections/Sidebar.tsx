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
        <div class="sidebar position-fixed top-0 start-0 h-100 bg-light border-end" style="width: 300px; max-width: 25%; min-width: 150px;">
            <div class="p-3">
                <button class="w-100 btn btn-primary" onClick={createNewChat}>New Chat</button>
            </div>
            <div class="p-3 d-flex flex-column h-100">
                <h5>Chat History</h5>
                {props.chats() ? (
                    <div class="list-group overflow-auto flex-grow-1 pb-5">
                        <For each={props.chats()}>
                            {(chat) => (
                                <ChatItem chat={chat} {...props} />
                            )}
                        </For>
                    </div>
                ) : (
                    <p>Loading chats...</p>
                )}
            </div>
        </div>
    );
};

export default Sidebar;

import { Component, createResource, createSignal, For } from "solid-js";
import chatService from "../services/ChatService";
import { Chat } from "../interfaces/Chat";
import { LLM } from "../interfaces/LLM";
import ChatItem from "./ChatItem";

interface SideBarProps {
    curChatId: () => number | null;
    setCurChatId: (id: number | null) => void;
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm: LLM) => void;
}

const fetchChats = async (): Promise<Chat[]> => {
    try {
        return await chatService.getChats();
    } catch (error) {
        console.error("Error fetching chats:", error);
        return [];
    }
};

const Sidebar: Component<SideBarProps> = (props) => {
    const [chats, { refetch }] = createResource(fetchChats);

    const createNewChat = async () => {
        if(props.selectedLLM()) {
            const newChat: Chat = { id: 0, title: "", lastActivity: Date.now(), llmId: props.selectedLLM()!.id };
            try {
                const createdChat = await chatService.createChat(newChat);
                props.setCurChatId(createdChat.id);
                refetch();
            } catch (error) {
                console.error("Error creating chat:", error);
            }
        }
    };

    return (
        <div class="sidebar position-fixed top-0 start-0 h-100 bg-light border-end" style="width: 300px; max-width: 25%; min-width: 150px;">
            <button class="btn btn-primary m-3" onClick={createNewChat}>New Chat</button>
            <div class="p-3">
                <h5>Chat History</h5>
                {chats() ? (
                    <div class="list-group">
                        <For each={chats()}>
                            {(chat) => (
                                <ChatItem chat={chat} refetch={refetch} {...props} />
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

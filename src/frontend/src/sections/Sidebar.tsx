import {Component, createResource} from "solid-js";
import ChatList from "./ChatList";
import chatService from "../services/ChatService"
import {Chat} from "../interfaces/Chat";

const fetchChats = async (): Promise<Chat[]> => {
    try {
        return await chatService.getChats();
    } catch (error) {
        console.error("Error fetching chats:", error);
        return [];
    }
};

const Sidebar: Component = () => {
    const [chats] = createResource(fetchChats);

    return (
        <div class="sidebar position-fixed top-0 start-0 h-100 bg-light border-end" style="width: 300px; max-width: 25%; min-width: 150px;">
            <button class="btn btn-primary m-3">New Chat</button>
            <div class="p-3">
                <h5>Chat History</h5>
                {chats() ? <ChatList chats={chats() ?? []} /> : <p>Loading chats...</p>}
            </div>
        </div>
    );
};

export default Sidebar;

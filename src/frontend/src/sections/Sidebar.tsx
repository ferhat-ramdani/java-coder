import { Component } from "solid-js";
import ChatList from "./ChatList";

const Sidebar: Component = () => {
    return (
        <div class="sidebar position-fixed top-0 start-0 h-100 bg-light border-end" style="width: 200px; max-width: 25%; min-width: 150px;">
            <button class="btn btn-primary m-3">New Chat</button>
            <div class="p-3">
                <h5>Chat History</h5>
                <ChatList />
            </div>
        </div>
    );
};

export default Sidebar;

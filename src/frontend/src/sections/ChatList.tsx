import { Component } from "solid-js";

const ChatList: Component = () => {
    const chats = [
        "Discussion on Java",
        "Frontend Frameworks",
        "Database Design",
        "API Development",
        "Unit Testing Strategies"
    ];

    return (
        <ul class="list-group">
            {chats.map((chat, index) => (
                <li class="list-group-item" style="cursor: pointer;">
                    {chat}
                </li>
            ))}
        </ul>
    );
};

export default ChatList;

import { Component } from "solid-js";
import ChatItem from "./ChatItem";  // Adjust the import path as necessary

const ChatList: Component = () => {
    const chats = [
        { date: "2024-09-20", time: "14:30", llm: "Model A", firstPrompt: "Discuss Java classes" },
        { date: "2024-09-21", time: "10:15", llm: "Model B", firstPrompt: "Explain REST APIs" },
        { date: "2024-09-22", time: "09:00", llm: "Model C", firstPrompt: "Best practices in frontend dev" }
    ];

    return (
        <div class="list-group">
            {chats.map((chat, index) => (
                <ChatItem
                    date={chat.date}
                    time={chat.time}
                    llm={chat.llm}
                    firstPrompt={chat.firstPrompt}
                />
            ))}
        </div>
    );
};

export default ChatList;

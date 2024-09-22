import { Component } from "solid-js";

type ChatItemProps = {
    date: string;
    time: string;
    llm: string;
    firstPrompt: string;
};

const ChatItem: Component<ChatItemProps> = (props) => {
    return (
        <div class="chat-item p-2 border-bottom">
            <div>
                <strong>{props.date} {props.time}</strong>
            </div>
            <div>LLM: {props.llm}</div>
            <div>
                <a href="#" class="text-decoration-none">{props.firstPrompt}</a>
            </div>
        </div>
    );
};

export default ChatItem;

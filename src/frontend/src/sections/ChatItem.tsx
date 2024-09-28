import { Component } from "solid-js";
import "../styles.css";

type ChatItemProps = {
    timestamp: string;
    llm: string;
    firstPrompt: string;
};

const ChatItem: Component<ChatItemProps> = (props) => {
    const handleClick = () => {
        console.log(`First prompt clicked: ${props.firstPrompt}`);
    };

    return (
        <div class="chat-item p-2 border-bottom hover-darken" onClick={handleClick}>
            <div>
                <strong>{props.firstPrompt}</strong>
            </div>
            <div>
                <strong>{props.timestamp}</strong>
            </div>
            <div>LLM: {props.llm}</div>
        </div>
    );
};

export default ChatItem;

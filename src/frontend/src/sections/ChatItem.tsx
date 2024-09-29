import { Component } from "solid-js";
import "../styles.css";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";

type ChatItemProps = {
    timestamp: string;
    llm: string;
    firstPrompt: string;
    curChatId: () => number | null;
    setCurChatId: () => void;
};

const ChatItem: Component<ChatItemProps> = (props) => {
    const handleClick = () => {
        props.setCurChatId();
    };

    const handleDelete = async () => {
        console.log("chat delete button is clicked!");
        if (props.curChatId()) {
            try {
                const chatId = props.curChatId()!;
                // retrieve all prompts of this chat
                const prompts = await PromptService.getPromptsByChatId(chatId);
                // delete them
                for (const prompt of prompts) {
                    await PromptService.deletePrompt(prompt.id);
                }
                // proceed to delete the chat
                await ChatService.deleteChat(chatId);
            } catch (error) {
                console.error("Failed to delete chat or prompts", error);
            }
        }
    };

    return (
        <div class="d-flex justify-content-between align-items-center">
            <div class="chat-item p-2 border-bottom hover-darken" onClick={handleClick}>
                <div>
                    <strong class="text-truncate">{props.firstPrompt} yes, that's life, but you know what ?</strong>
                </div>
                <div>
                    <strong>{props.timestamp}</strong>
                </div>
                <div>LLM: {props.llm}</div>
            </div>
            <button class="btn btn-danger ms-2" onClick={handleDelete}>Delete</button>
        </div>
    );
};

export default ChatItem;

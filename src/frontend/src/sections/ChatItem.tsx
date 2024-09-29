import { Component } from "solid-js";
import "../styles.css";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import {LLM} from "../interfaces/LLM";
import LLMService from "../services/LLMService";

type ChatItemProps = {
    timestamp: string;
    llm: string;
    firstPrompt: string;
    curChatId: () => number | null;
    setCurChatId: (chatId: number) => void;
    chatId: number;
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm : LLM) => void;
};

const ChatItem: Component<ChatItemProps> = (props) => {
    const handleClick = async () => {
        props.setCurChatId(props.chatId);
        try {
            const chat = await ChatService.getChatById(props.chatId);
            const llm = await LLMService.getLlmById(chat.llmId);
            props.setSelectedLLM(llm);
        } catch (error) {
            console.error("Error fetching chat or LLM:", error);
        }
    };

    const handleDelete = async () => {
        console.log("chat delete button is clicked!");
        if (props.curChatId()) {
            try {
                const chatId = props.curChatId()!;
                const prompts = await PromptService.getPromptsByChatId(chatId);
                for (const prompt of prompts) {
                    await PromptService.deletePrompt(prompt.id);
                }
                await ChatService.deleteChat(chatId);
            } catch (error) {
                console.error("Failed to delete chat or prompts", error);
            }
        }
    };

    return (
        <div
            class={`d-flex justify-content-between align-items-center hover-darken 
            ${props.curChatId() === props.chatId ? 'darkened' : 'brightened'}`}
        >
            <div class="chat-item p-2 border-bottom" onClick={handleClick}>
                <div>
                    <strong class="text-truncate">{props.firstPrompt} yes, that's life, but you know what ?</strong>
                </div>
                <div>{props.timestamp}</div>
                <div>LLM: {props.llm}</div>
            </div>
            <button class={`btn btn-danger ms-2 me-2 ${props.curChatId() === props.chatId ? 'd-block' : 'd-none'}`} onClick={handleDelete}>Delete</button>
        </div>
    );
};

export default ChatItem;

import { Component, createResource } from "solid-js";
import "../styles.css";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import { LLM } from "../interfaces/LLM";
import LLMService from "../services/LLMService";
import { TimestampUtils } from '../services/TimeStampUtils';
import { Chat } from "../interfaces/Chat";

type ChatItemProps = {
    chat: Chat;
    refetch: () => Chat[] | Promise<Chat[] | undefined> | null | undefined;
    curChatId: () => number | null;
    setCurChatId: (chatId: number | null) => void;
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm: LLM | null) => void;
};

const ChatItem: Component<ChatItemProps> = (props) => {
    const { chat } = props;
    const timestamp = TimestampUtils.toHumanReadable(chat.lastActivity);

    const [fetchedFirstPrompt] = createResource(async () => {
       try {
           return await PromptService.getFirstPromptOfChat(chat.id);
       } catch (error) {
           console.error("Error fetching first prompt of chat", error);
           return null;
       }
    });

    const [fetchedLLM] = createResource(async () => {
        try {
            return await LLMService.getLlmById(chat.llmId);
        } catch (error) {
            console.error("Error fetching LLM:", error);
            return null;
        }
    });

    const handleClick = async () => {
        props.setCurChatId(chat.id);
        if(fetchedLLM()) {
            props.setSelectedLLM(fetchedLLM()!);
        } else {
            try {
                const fetchedLLM = await LLMService.getLlmById(chat.llmId);
                props.setSelectedLLM(fetchedLLM);
            } catch (error) {
                console.error("Error fetching chat or LLM:", error);
            }
        }
    };

    const handleDelete = async () => {
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
        props.setCurChatId(null);
        props.refetch();
        props.setSelectedLLM(null);
    };

    return (
        <div
            class={`d-flex justify-content-between align-items-center hover-darken 
            ${props.curChatId() === chat.id ? 'darkened' : 'brightened'}`}
        >
            <div class="chat-item p-2 border-bottom" onClick={handleClick}>
                <div>
                    <strong class="text-truncate">{fetchedFirstPrompt.loading ? "Prompt loading" :
                        fetchedFirstPrompt() ? fetchedFirstPrompt()!.message : "- No Title -"}</strong>
                </div>
                <div>{timestamp}</div>
                <div>
                    {fetchedLLM.loading ? "LLM loading..." : fetchedLLM() ? `${fetchedLLM()!.name}` : "- No LLM -"}
                </div>
            </div>
            <button class={`btn btn-danger ms-2 me-2 ${props.curChatId() === chat.id ? 'd-block' : 'd-none'}`} onClick={handleDelete}>
                Delete
            </button>
        </div>
    );
};

export default ChatItem;

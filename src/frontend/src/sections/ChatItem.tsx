import {Component, createEffect, createResource} from "solid-js";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import LLMService from "../services/LLMService";
import { TimestampUtils } from '../services/TimeStampUtils';
import { Chat } from "../interfaces/Chat";
import {useAppContext} from "../Context";
import "../styles.css";
import {Prompt} from "../interfaces/Prompt";
import promptService from "../services/PromptService";

const fetchPromptsOfChat = async (chatId : number): Promise<Prompt[]> => {
    try {
        return await promptService.getPromptsByChatId(chatId);
    } catch (error) {
        console.error("Failed to fetch prompts", error);
        return [];
    }
}

const ChatItem: Component<{chat : Chat}> = (prop) => {
    const chat : Chat = prop.chat;
    const [{curChatPrompts, curChatId, selectedLLM, chats}] = useAppContext();
    const timestamp = TimestampUtils.toHumanReadable(chat.lastActivity);

    const [fetchedFirstPrompt] = createResource(async () => {
       try {
           return await PromptService.getFirstPromptOfChat(chat.id);
       } catch (error) {
           console.error("Error fetching first prompt of chat", error);
           return null;
       }
    });

    const [fetchedLLM, {refetch}] = createResource(async () => {
        try {
            return await LLMService.getLlmById(chat.llmId);
        } catch (error) {
            console.error("Error fetching LLM:", error);
            return null;
        }
    });

    const handleClick = async () => {
        curChatId.setter(chat.id);
        if(fetchedLLM()) {
            selectedLLM.setter(fetchedLLM()!);
        } else {
            refetch()
            selectedLLM.setter(fetchedLLM()!);
        }
        const chatId = curChatId.accessor();
        if (chatId) {
            const chatPrompts = await fetchPromptsOfChat(chatId);
            curChatPrompts.setter(chatPrompts);
        } else {
            curChatPrompts.setter([]);
        }
    };

    const handleDelete = async () => {
        const chatId = curChatId.accessor();
        if (chatId) {
            try {
                const prompts = await PromptService.getPromptsByChatId(chatId);
                for (const prompt of prompts) {
                    await PromptService.deletePrompt(prompt.id);
                }
                await ChatService.deleteChat(chatId);
            } catch (error) {
                console.error("Failed to delete chat or prompts", error);
            }
        }
        curChatId.setter(null);
        selectedLLM.setter(null);
        curChatPrompts.setter([]);
        chats.mutator(chats.resource()?.filter(Chat => Chat.id !== chatId));
    };

    return (
        <div
            class={`d-flex justify-content-between align-items-center hover-darken rounded position-relative
            ${curChatId.accessor() === chat.id ? 'darkened' : 'brightened'}`}
        >
            <div class="p-2 border-bottom w-75 pg-warning" onClick={handleClick}>
                <div class="w-100 text-truncate">
                    <strong>
                        {fetchedFirstPrompt.loading ? "Prompt loading" :
                        fetchedFirstPrompt() ? fetchedFirstPrompt()!.message : "- No Title -"}
                    </strong>
                </div>
                <div>{timestamp}</div>
                <div>
                    {fetchedLLM.loading ? "LLM loading..." : fetchedLLM() ? `${fetchedLLM()!.name}` : "- No LLM -"}
                </div>
            </div>
            <div class="position-absolute me-2" style="right: 0">
                <button class={`btn btn-danger ${curChatId.accessor() === chat.id ? 'd-block' : 'd-none'}`} onClick={handleDelete}>
                    Delete
                </button>
            </div>
        </div>
    );
};

export default ChatItem;

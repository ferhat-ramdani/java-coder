import {Component, createResource, Show} from "solid-js";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import LLMService from "../services/LLMService";
import { Utils } from '../services/Utils';
import { Chat } from "../interfaces/Chat";
import {useAppContext} from "../Context";
import "../styles.css";
import {Prompt} from "../interfaces/Prompt";
import promptService from "../services/PromptService";
import {Spinner, SpinnerSmall} from "./Spinner";

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
    const timestamp = Utils.toHumanReadable(chat.lastActivity);


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

    const showDelete = () => {
        if (curChatId.accessor() != chat.id) {
            document.getElementById(`${chat.id}-remove`)?.classList.toggle('d-block');
            document.getElementById(`${chat.id}-remove`)?.classList.toggle('d-none');
        }
    }

    const handleDelete = async () => {
        const chatId = chat.id;
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
        <div class={`d-flex justify-content-between align-items-center hover-darken rounded position-relative border-bottom
            ${curChatId.accessor() === chat.id ? 'darkened' : 'brightened'}`} onClick={handleClick}
             onMouseEnter={() => {
                 showDelete();
             }}
             onMouseLeave={() => {
                 showDelete();
             }}>
            <div class="p-2 w-100 pg-warning" >
                <div class="w-100 text-truncate">
                    <Show when={!fetchedFirstPrompt.loading} fallback={<SpinnerSmall text="Prompt loading"/>} keyed>
                        <strong>{fetchedFirstPrompt() ? fetchedFirstPrompt()!.message : "- No Title -"}</strong>
                    </Show>
                </div>
                <div>{timestamp}</div>
                <Show when={!fetchedLLM.loading} fallback={<SpinnerSmall text="LLM loading"/>} keyed>
                    <div>
                        {fetchedLLM() ? `${fetchedLLM()!.name}` : "- No LLM -"}
                    </div>
                </Show>
            </div>
            <div class="my-2">
                <button id={`${chat.id}-remove`} type="button" className={`btn-close ${curChatId.accessor() === chat.id ? 'd-block' : 'd-none'}`}
                        onClick={handleDelete}>
                </button>
            </div>
        </div>
    );
};

export default ChatItem;

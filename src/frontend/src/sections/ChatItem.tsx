import {Component, createResource, Show} from "solid-js";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import LLMService from "../services/LLMService";
import {Utils} from '../services/Utils';
import {Chat} from "../interfaces/Chat";
import {useAppContext} from "../Context";
import {SpinnerSmall} from "./Spinner";
import {A} from "@solidjs/router";

const ChatItem: Component<{ chat: Chat }> = (prop) => {
    const chat: Chat = prop.chat;
    const [{selectedLLM, chats}] = useAppContext();
    const timestamp = Utils.toHumanReadable(chat.lastActivity);

    const [fetchedLLM, {refetch}] = createResource(async () => {
        try {
            return await LLMService.getLlmById(chat.llmId);
        } catch (error) {
            console.error("Error fetching LLM:", error);
            return null;
        }
    });

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
        selectedLLM.setter(null);
        chats.mutator(chats.resource()?.filter(Chat => Chat.id !== chatId));
    };

    return (
        <li class={`list-group-item d-flex justify-content-between align-items-center hover-darken`}>
            <A href={`/chats/${chat.id}`} class={'no-decoration text-truncate w-100'} activeClass="text-decoration-none"
               inactiveClass="text-decoration-none" end>
                <div class="text-truncate">
                    <h5 class={`mb-1 text-truncate`}>{chat.title ? chat.title : "- No Prompt -"}</h5>
                    <Show when={!fetchedLLM.loading} fallback={<SpinnerSmall text="LLM loading"/>} keyed>
                        <small class={`text-body-secondary text-truncate`}>Date: {timestamp} | LLM: {fetchedLLM() ? `${fetchedLLM()!.name}` : "- No LLM -"}</small>
                    </Show>
                </div>
            </A>
            <button id={`${chat.id}-remove`} type="button" aria-label="Close"
                    class={`btn-close`}
                    onClick={handleDelete}>
            </button>
        </li>);
};

export default ChatItem;

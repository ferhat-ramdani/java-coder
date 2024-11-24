import { Component, createResource, Show } from "solid-js";
import ChatService from "../services/ChatService";
import PromptService from "../services/PromptService";
import LLMService from "../services/LLMService";
import { Utils } from "../services/Utils";
import { Chat } from "../interfaces/Chat";
import { useAppContext } from "../Context";
import { SpinnerSmall } from "./Spinner";
import { A } from "@solidjs/router";

const deleteChat = async (chat: Chat, chats: any) => {
    if (chat.id) {
        try {
            const prompts = await PromptService.getPromptsByChatId(chat.id);
            await Promise.all(prompts.map((prompt) => PromptService.deletePrompt(prompt.id)));
            await ChatService.deleteChat(chat.id);
            chats.mutator(chats.resource()?.filter(({ id }: Chat) => id !== chat.id));
        } catch (error) {
            console.error("Failed to delete chat or prompts", error);
        }
    }
};

const ChatItem: Component<{ chat: Chat }> = ({ chat }) => {
    const [{ chats }] = useAppContext();
    const timestamp = Utils.toHumanReadable(chat.lastActivity);
    const [fetchedLLM] = createResource(() => LLMService.getLlmById(chat.llmId).catch((error) => {
        console.error("Error fetching LLM:", error);
        return null;
    }));

    return (
        <li class="list-group-item d-flex justify-content-between align-items-center hover-darken">
            <A
                href={`/chats/${chat.id}`}
                class="no-decoration text-truncate w-100"
                activeClass="text-decoration-none"
                inactiveClass="text-decoration-none"
                end
            >
                <div class="text-truncate">
                    <h5 class="mb-1 text-truncate">{chat.title || "- No Prompt -"}</h5>
                    <Show when={!fetchedLLM.loading} fallback={<SpinnerSmall text="LLM loading" />} keyed>
                        <small class="text-body-secondary text-truncate">
                            Date: {timestamp} | LLM: {fetchedLLM()?.name || "- No LLM -"}
                        </small>
                    </Show>
                </div>
            </A>
            <button
                id={`${chat.id}-remove`}
                type="button"
                aria-label="Close"
                class="btn-close"
                onClick={() => deleteChat(chat, chats)}
            ></button>
        </li>
    );
};

export default ChatItem;

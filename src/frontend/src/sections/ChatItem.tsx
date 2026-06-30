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
        <li class="list-group-item d-flex justify-content-between align-items-center bg-surface-hover hover-lift p-3 border-custom border-bottom">
            <A
                href={`/chats/${chat.id}`}
                class="text-decoration-none text-truncate w-100 text-main"
                activeClass="text-decoration-none"
                inactiveClass="text-decoration-none"
                end
            >
                <div class="text-truncate d-flex align-items-center">
                    <i class="bi bi-chat-left-text text-primary me-3 fs-5"></i>
                    <div>
                        <h5 class="mb-1 text-truncate fw-semibold text-main">{chat.title || "- New Chat -"}</h5>
                        <Show when={!fetchedLLM.loading} fallback={<SpinnerSmall text="Loading info" />} keyed>
                            <small class="text-muted-custom text-truncate d-block">
                                <i class="bi bi-clock me-1"></i>{timestamp} <span class="mx-2">•</span> <i class="bi bi-robot me-1"></i>{fetchedLLM()?.name || "- No LLM -"}
                            </small>
                        </Show>
                    </div>
                </div>
            </A>
            <button
                id={`${chat.id}-remove`}
                type="button"
                aria-label="Close"
                class="btn-close ms-3"
                onClick={() => deleteChat(chat, chats)}
            ></button>
        </li>
    );
};

export default ChatItem;

import { Component, For } from "solid-js";
import ChatItem from "./ChatItem";
import { Chat } from "../interfaces/Chat";
import { TimestampUtils } from '../services/TimeStampUtils';
import {LLM} from "../interfaces/LLM";

interface ChatListProps {
    chats: Chat[];
    setCurChatId: (id: number) => void;
    curChatId: () => number | null;
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm : LLM) => void;
}

const ChatList: Component<ChatListProps> = (props) => {
    return (
        <div class="list-group">
            <For each={props.chats}>
                {(chat) => (
                    <ChatItem
                        timestamp={TimestampUtils.toHumanReadable(chat.lastActivityTimestamp)}
                        llm={chat.llmId.toString()}
                        firstPrompt={`Frist prompt: ${chat.id}`}
                        chatId={chat.id}
                        {...props}
                    />
                )}
            </For>
        </div>
    );
};

export default ChatList;

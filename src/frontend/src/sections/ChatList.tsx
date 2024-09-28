import { Component, For } from "solid-js";
import ChatItem from "./ChatItem";
import { Chat } from "../interfaces/Chat";
import { TimestampUtils } from '../services/TimeStampUtils';

interface ChatListProps {
    chats: Chat[];
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
                    />
                )}
            </For>
        </div>
    );
};

export default ChatList;

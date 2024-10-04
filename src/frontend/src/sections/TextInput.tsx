import {Component, createSignal, Resource, Setter} from "solid-js";
import promptService from "../services/PromptService";
import { Prompt } from "../interfaces/Prompt";
import {Chat} from "../interfaces/Chat";
import chatService from "../services/ChatService";
import {useAppContext} from "../Context";

const TextInput: Component = () => {
    const [{curChatId, selectedLLM, curChatPrompts, chats}] = useAppContext();
    const [message, setMessage] = createSignal("");

    const handleSend = async () => {
        if (!message().trim()) {
            return;
        }
        if (!curChatId.accessor()) {
            if(!selectedLLM.accessor()) {
                alert("Please select an LLM Model");
            } else {
                await createNewChat();
                await insertNewPrompt();
                setMessage("");
            }
        } else if(selectedLLM.accessor()) {
            await insertNewPrompt();
            setMessage("");
        }
    };

    const createNewChat = async () => {
        if(selectedLLM.accessor()) {
            const newChat: Chat = { id: 0, title: "", lastActivity: Date.now(), llmId: selectedLLM.accessor()!.id };
            try {
                const createdChat = await chatService.createChat(newChat);
                curChatId.setter(createdChat.id);
                // chats.mutator(prev => [...(prev!), createdChat]);
                chats.refetcher();
            } catch (error) {
                console.error("Error creating chat:", error);
            }
        } else {
            alert("Please select an LLM model!");
        }
    };

    const insertNewPrompt = async () => {
        const newPrompt: Prompt = {
            id: 0, // random value that should not be used
            message: message().trim(),
            authorType: "USER",
            chatId: curChatId.accessor()!,
        };

        try {
            await promptService.createPrompt(newPrompt);
            curChatPrompts.setter(prev => [...prev, newPrompt]);
        } catch (error) {
            console.error("Failed to create prompt:", error);
        }
    }

    return (
        <div class="input-group d-flex justify-content-center">
            <div class="input-group w-50 mb-2 d-flex">
              <textarea
                  class="form-control rounded-start"
                  rows="1"
                  placeholder="Type your message here..."
                  style="resize: none;"
                  value={message()}
                  onInput={(e) => setMessage(e.currentTarget.value)}
                  onKeyPress={(e) => {
                      if (e.key === 'Enter') {
                          e.preventDefault();
                          handleSend();
                      }
                  }}
              ></textarea>
                <button
                    class="btn btn-primary rounded-end"
                    type="button"
                    onClick={handleSend}
                >
                    Send
                </button>
            </div>
        </div>
    );
};

export default TextInput;

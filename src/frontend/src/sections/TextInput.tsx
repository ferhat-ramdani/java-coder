import {Component, createSignal, Resource, Setter} from "solid-js";
import promptService from "../services/PromptService";
import { Prompt } from "../interfaces/Prompt";
import {LLM} from "../interfaces/LLM";
import {Chat} from "../interfaces/Chat";
import chatService from "../services/ChatService";

interface TextInputProps {
    curChatId: () => number | null;
    setCurChatId: (id: number | null) => void;
    setRefreshPrompts: Setter<boolean>;
    selectedLLM: () => LLM | null;
    refetch: () => Chat[] | Promise<Chat[] | undefined> | null | undefined;
    chats: Resource<Chat[]>;
}

const TextInput: Component<TextInputProps> = (props) => {
    const [message, setMessage] = createSignal("");

    const handleSend = async (setRefreshPrompts: Setter<boolean>) => {
        if (!message().trim()) {
            return;
        }
        if (!props.curChatId()) {
            if(!props.selectedLLM) {
                alert("Please select an LLM Model");
            } else {
                await createNewChat();
                await createPrompt();
            }
        } else if(props.selectedLLM()) {
            await createPrompt();
        }
        setMessage("");
        setRefreshPrompts(true);
        props.refetch();
    };

    const createNewChat = async () => {
        if(props.selectedLLM()) {
            const newChat: Chat = { id: 0, title: "", lastActivity: Date.now(), llmId: props.selectedLLM()!.id };
            try {
                const createdChat = await chatService.createChat(newChat);
                props.setCurChatId(createdChat.id);
            } catch (error) {
                console.error("Error creating chat:", error);
            }
        } else {
            alert("Please select an LLM model!");
        }
    };

    const createPrompt = async () => {
        const newPrompt: Prompt = {
            id: 0, // random value that should not be used
            message: message().trim(),
            authorType: "USER",
            chatId: props.curChatId()!,
        };

        try {
            await promptService.createPrompt(newPrompt);
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
                  //disabled={!props.curChatId()}
                  onKeyPress={(e) => {
                      if (e.key === 'Enter') {
                          e.preventDefault();
                          handleSend(props.setRefreshPrompts);
                      }
                  }}
              ></textarea>
                <button
                    class="btn btn-primary rounded-end"
                    type="button"
                    onClick={() => handleSend(props.setRefreshPrompts)}
                    //disabled={!props.curChatId()}
                >
                    Send
                </button>
            </div>
        </div>
    );
};

export default TextInput;

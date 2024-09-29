import {Component, createSignal, Setter} from "solid-js";
import promptService from "../services/PromptService";
import { Prompt } from "../interfaces/Prompt";

interface TextInputProps {
    curChatId: () => number | null;
    setRefreshPrompts: Setter<boolean>;
}

const TextInput: Component<TextInputProps> = (props) => {
    const [message, setMessage] = createSignal("");

    const handleSend = async (setter: Setter<boolean>) => {
        if (!props.curChatId() || !message().trim()) {
            return;
        }

        const newPrompt: Prompt = {
            id: 0, // random value that should not be used
            message: message().trim(),
            authorType: "USER",
            llmResponse: "",
            chatId: props.curChatId()!,
        };

        try {
            await promptService.createPrompt(newPrompt);
            setMessage("");
            setter(true);
        } catch (error) {
            console.error("Failed to create prompt:", error);
        }
    };

    return (
        <div class="input-group w-50 mb-2">
            <textarea
                  class="form-control rounded-start"
                  rows="1"
                  placeholder="Type your message here..."
                  style="resize: none;"
                  value={message()}
                  onInput={(e) => setMessage(e.currentTarget.value)}
                  disabled={!props.curChatId()}
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
                disabled={!props.curChatId()}
            >
                Send
            </button>
        </div>
    );
};

export default TextInput;

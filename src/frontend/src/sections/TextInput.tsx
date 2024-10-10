import {Component, createSignal, Resource, Setter} from "solid-js";
import promptService from "../services/PromptService";
import {createPrompt, Prompt} from "../interfaces/Prompt";
import {Chat} from "../interfaces/Chat";
import chatService from "../services/ChatService";
import {useAppContext} from "../Context";
import {AuthorType} from "../interfaces/AuthorType";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";

const TextInput: Component = () => {
    const [{curChatId, selectedLLM, curChatPrompts, chats}] = useAppContext();
    const [message, setMessage] = createSignal("");

    const fetchLLMResponse = async () => {
        const messageToSend = message();
        setMessage("");
        try {
            const newPrompt: Prompt = createPrompt(messageToSend, AuthorType.USER, curChatId.accessor()!);
            await insertNewPrompt(newPrompt);

            const response = await generatorService.generateResponseFromLLM(newPrompt);

            // const execOutput = await generatorService.executeClass(response);
            // console.log(execOutput);

            console.log("execution output : ");


            const llmPromt: Prompt = createPrompt(response, AuthorType.LLM, curChatId.accessor()!);
            await insertNewPrompt(llmPromt);

            // const eventSource = new EventSource(`http://localhost:8080/api/gen/test`);
            //
            // eventSource.onmessage = (event) => {
            //     const newMessage = event.data;
            //     console.log("msg", event);
            //     console.log("new", newMessage);
            // };

            // await insertNewPrompt(response, "LLM");
        } catch (error) {
            console.error("Error fetching llm response:", error);
        }
    }

    const handleSend = async () => {
        if (!message().trim()) {
            return;
        }
        if (!curChatId.accessor()) {
            if(!selectedLLM.accessor()) {
                Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
            } else {
                await createNewChat(message());
                await fetchLLMResponse();
            }
        } else if(selectedLLM.accessor()) {
            await fetchLLMResponse();
        }
    };

    const createNewChat = async (title = "") => {
        if(selectedLLM.accessor()) {
            const newChat: Chat = { id: 0, title: title, lastActivity: Date.now(), llmId: selectedLLM.accessor()!.id };
            try {
                const createdChat = await chatService.createChat(newChat);
                curChatId.setter(createdChat.id);
                // chats.mutator(prev => [...(prev!), createdChat]);
                chats.refetcher();
            } catch (error) {
                console.error("Error creating chat:", error);
            }
        } else {
            Utils.showToast("Error", "Please select an LLM Model", "danger", "bi-exclamation-triangle");
        }
    };

    const insertNewPrompt = async (prompt: Prompt, send: boolean = false) => {
        try {
            if(send) {
                await promptService.createPrompt(prompt);
            }
            curChatPrompts.setter(prev => [...prev, prompt]);
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
                <button class="btn btn-primary rounded-end" type="button" onClick={handleSend}>
                    <i class="bi bi-send-fill"></i>
                </button>
            </div>
        </div>
    );
};

export default TextInput;

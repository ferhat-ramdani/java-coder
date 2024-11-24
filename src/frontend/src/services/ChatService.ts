import Config from '../Config';
import { Chat } from "../interfaces/Chat";
import {Utils} from "./Utils";

class ChatService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api`;

    async getChats(): Promise<Chat[]> {
        const response = await fetch(`${this.apiUrl}/chat`);

        await Utils.showErrorToast(response, "Error during chats retrieval.");

        return await response.json();
    }

    async getChatById(id: number): Promise<Chat> {
        const response = await fetch(`${this.apiUrl}/chat/${id}`);
        if (!response.ok) {
            throw new Error(`Error in GET query : ${response.statusText}`);
        }
        return await response.json();
    }

    async createChat(chat: Chat) : Promise<Chat> {
        const response = await fetch(`${this.apiUrl}/chat`, Utils.createRequestInit(chat, 'POST'));

        await Utils.showErrorToast(response, "Error during chat creation.");

        return await response.json();
    }

    async deleteChat(id: number) {
        const response = await fetch(`${this.apiUrl}/chat/${id}`, {
            method: 'DELETE',
        });

        await Utils.showErrorToast(response, "Error during chat deletion.");
    }
}

const chatService = new ChatService();
export default chatService;

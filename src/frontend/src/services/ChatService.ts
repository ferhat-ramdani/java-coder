import Config from '../Config';
import { Chat } from "../interfaces/Chat";
import {Utils} from "./Utils";

class ChatService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api`;

    async getChats(): Promise<Chat[]> {
        const response = await fetch(`${this.apiUrl}/chat`);
        const data = await response.json();
        if (!response.ok) {
            console.log(`Error in GET query : ${response.statusText}`);
            throw new Error(`Error in GET query : ${response.statusText}`);
        }
        return data;
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
        const data = await response.json();

        if (!response.ok) {
            throw new Error(`Error in POST query : ${response.statusText}`);
        }
        return data;
    }

    async updateChat(chat: Chat){
        const response = await fetch(`${this.apiUrl}/chat`, Utils.createRequestInit(chat, 'PUT'));

        if (!response.ok) {
            throw new Error(`Error during PUT query : ${response.statusText}`);
        }
    }

    async deleteChat(id: number) {
        const response = await fetch(`${this.apiUrl}/chat/${id}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            throw new Error(`Error during DELETE query : ${response.statusText}`);
        }
    }
}

const chatService = new ChatService();
export default chatService;

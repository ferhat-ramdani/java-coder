import Config from '../Config';
import { Chat } from "../interfaces/Chat";

class ChatService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/db`;

    async getChats(): Promise<Chat[]> {
        const response = await fetch(`${this.apiUrl}/chat`);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return data;
    }

    async getChatById(id: number): Promise<Chat> {
        const response = await fetch(`${this.apiUrl}/chat/${id}`);
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return await response.json();
    }

    async createChat(chat: Chat): Promise<Chat> {
        const response = await fetch(`${this.apiUrl}/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(chat),
        });

        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel POST : ${response.statusText}`);
        }
        return await response.json();
    }

    async updateChat(chat: Chat): Promise<Chat> {
        const response = await fetch(`${this.apiUrl}/chat`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(chat),
        });

        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel PUT : ${response.statusText}`);
        }
        return await response.json();
    }

    async deleteChat(id: number): Promise<void> {
        const response = await fetch(`${this.apiUrl}/chat/${id}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel DELETE : ${response.statusText}`);
        }
    }
}

const chatService = new ChatService();
export default chatService;

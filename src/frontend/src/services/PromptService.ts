import Config from '../Config';
import { Prompt } from "../interfaces/Prompt";

class PromptService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/db`;

    async getPrompts(): Promise<Prompt[]> {
        const response = await fetch(`${this.apiUrl}/prompt`);
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return await response.json();
    }

    async getPromptById(id: number): Promise<Prompt> {
        const response = await fetch(`${this.apiUrl}/prompt/${id}`);
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return await response.json();
    }

    async getPromptsByChatId(chatId: number): Promise<Prompt[]> {
        const response = await fetch(`${this.apiUrl}/chat/${chatId}/prompts`);
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return await response.json();
    }

    async createPrompt(prompt: Prompt) {
        console.log("stringified prompt : ");
        console.log(JSON.stringify(prompt));
        const response = await fetch(`${this.apiUrl}/prompt`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(prompt),
        });

        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel POST : ${response.statusText}`);
        }
    }

    async updatePrompt(prompt: Prompt) {
        const response = await fetch(`${this.apiUrl}/prompt`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(prompt),
        });

        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel PUT : ${response.statusText}`);
        }
    }

    async deletePrompt(id: number) {
        const response = await fetch(`${this.apiUrl}/prompt/${id}`, {
            method: 'DELETE',
        });

        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel DELETE : ${response.statusText}`);
        }
    }
}

const promptService = new PromptService();
export default promptService;

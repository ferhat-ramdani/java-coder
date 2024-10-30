import Config from '../Config';
import { Prompt } from "../interfaces/Prompt";
import {Utils} from "./Utils";
import {onCleanup} from "solid-js";

class PromptService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/prompt`;

    async getPrompts(): Promise<Prompt[]> {
        const response = await fetch(`${this.apiUrl}`);
        if (!response.ok) {
            throw new Error(`Error during GET query : ${response.statusText}`);
        }
        return await response.json();
    }

    async getPromptById(id: number): Promise<Prompt> {
        const response = await fetch(`${this.apiUrl}/${id}`);
        if (!response.ok) {
            throw new Error(`Error during GET query : ${response.statusText}`);
        }
        return await response.json();
    }

    async getPromptsByChatId(chatId: number): Promise<Prompt[]> {
        const response = await fetch(`${this.apiUrl}/bychat/${chatId}`);

        await Utils.showErrorToast(response, "Error during prompts retrieval.");

        return await response.json();
    }

    async createPrompt(prompt: Prompt) {
        const response = await fetch(`${this.apiUrl}`, Utils.createRequestInit(prompt, 'POST'));

        await Utils.showErrorToast(response, "Error during prompt creation.");
    }

    async updatePrompt(prompt: Prompt) {
        const response = await fetch(`${this.apiUrl}`, Utils.createRequestInit(prompt, 'PUT'));

        if (!response.ok) {
            throw new Error(`Error during PUT query : ${response.statusText}`);
        }
    }

    async deletePrompt(id: number) {
        const response = await fetch(`${this.apiUrl}/${id}`, {
            method: 'DELETE',
        });

        await Utils.showErrorToast(response, "Error during prompt deletion.");
    }

    async testProgressive() {
        const eventSource = new EventSource(`${this.apiUrl}/test_progressive`);
        eventSource.onmessage = (event) => {
            console.log(event.data);
        };

        onCleanup(() => eventSource.close());

    }
}

const promptService = new PromptService();
export default promptService;

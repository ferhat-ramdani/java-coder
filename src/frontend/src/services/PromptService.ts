import Config from '../Config';
import { Prompt } from "../interfaces/Prompt";
import {Utils} from "./Utils";

class PromptService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/prompt`;

    async getPromptsByChatId(chatId: number): Promise<Prompt[]> {
        const response = await fetch(`${this.apiUrl}/bychat/${chatId}`);
        await Utils.showErrorToast(response, "Error during prompts retrieval.");
        return await response.json();
    }

    async createPrompt(prompt: Prompt) {
        const response = await fetch(`${this.apiUrl}`, Utils.createRequestInit(prompt, 'POST'));

        await Utils.showErrorToast(response, "Error during prompt creation.");
    }

    async deletePrompt(id: number) {
        const response = await fetch(`${this.apiUrl}/${id}`, {
            method: 'DELETE',
        });

        await Utils.showErrorToast(response, "Error during prompt deletion.");
    }
}

const promptService = new PromptService();
export default promptService;

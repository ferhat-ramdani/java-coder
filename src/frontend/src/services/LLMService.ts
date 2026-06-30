import Config from '../Config';
import {LLM} from "../interfaces/LLM";
import {Utils} from "./Utils";


class LLMService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/llm`;
    
    getPullUrl(id: number): string {
        return `${this.apiUrl}/pull/${id}`;
    }
    
    async getLLMS(): Promise<LLM[]> {
        const response = await fetch(`${this.apiUrl}`);

        await Utils.showErrorToast(response, "Error during LLMs retrieval.");

        return await response.json();
    }

    async getFirstLLM(): Promise<LLM> {
        const response = await fetch(`${this.apiUrl}/first/llm`);

        await Utils.showErrorToast(response, "Error during LLM retrieval.");

        return await response.json();
    }

    async getLlmById(id: number): Promise<LLM> {
        const response = await fetch(`${this.apiUrl}/${id}`);

        await Utils.showErrorToast(response, "Error during LLM retrieval.");

        return await response.json();
    }

    async addLLM(llm: Partial<LLM>): Promise<LLM> {
        const response = await fetch(`${this.apiUrl}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(llm)
        });

        await Utils.showErrorToast(response, "Error adding new LLM.");

        return await response.json();
    }
}

const llmService = new LLMService();
export default llmService;

import Config from '../Config';
import {LLM} from "../interfaces/LLM";
import {Prompt} from "../interfaces/Prompt";
import {Utils} from "./Utils";


class LLMService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/llm`;
    
    async getLLMS(): Promise<LLM[]> {
        const response = await fetch(`${this.apiUrl}`);

        await Utils.showErrorToast(response, "Error during LLMs retrieval.");

        return await response.json();
    }

    async getLlmById(id: number): Promise<LLM> {
        const response = await fetch(`${this.apiUrl}/${id}`);

        await Utils.showErrorToast(response, "Error during LLM retrieval.");

        return await response.json();
    }
}

const llmService = new LLMService();
export default llmService;

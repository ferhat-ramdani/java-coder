import Config from '../Config';
import {LLM} from "../interfaces/LLM";
import {Prompt} from "../interfaces/Prompt";
import {Utils} from "./Utils";


class LLMService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/llm`;
    
    async getLLMS(): Promise<LLM[]> {
        const response = await fetch(`${this.apiUrl}`);
        if (!response.ok) {
            throw new Error(`Error during GET query : ${response.statusText}`);
        }
        return await response.json();
    }

    async getLlmById(id: number): Promise<LLM> {
        const response = await fetch(`${this.apiUrl}/${id}`);
        if (!response.ok) {
            throw new Error(`Error during GET query : ${response.statusText}`);
        }
        return await response.json();
    }
}

const llmService = new LLMService();
export default llmService;

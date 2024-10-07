import Config from '../Config';
import {LLM} from "../interfaces/LLM";
import {Chat} from "../interfaces/Chat";
import {Prompt} from "../interfaces/Prompt";

class LLMService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/llm/`;
    
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

    async generateResponseFromLLM(prompt: Prompt): Promise<string>{
        const data = await fetch(`${this.config.getBackendUrl()}/api/gen/class`, {
            method: 'POST',
            headers: {
                'content-type' : 'application/json',
            },
            body: JSON.stringify(prompt)
        });
        if (!data.ok) {
            throw new Error(`Error during GET query : ${data.statusText}`);
        }
        return data.text();
    }

    async executeClass(code : string): Promise<string>{
        const data = await fetch(`${this.config.getBackendUrl()}/api/gen/exec`, {
            method: 'POST',
            headers: {
                'content-type' : 'application/json',
            },
            body: code
        });
        if (!data.ok) {
            throw new Error(`Error during GET query : ${data.statusText}`);
        }
        return data.text();
    }
}

const llmService = new LLMService();
export default llmService;

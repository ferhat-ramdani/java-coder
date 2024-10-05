import Config from '../Config';
import {LLM} from "../interfaces/LLM";
import {Chat} from "../interfaces/Chat";

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

    async generateResponseFromLLM(queryText: string): Promise<string>{
        const data = await fetch(`${this.apiUrl}/class`, {
            method: 'POST',
            headers: {
                'content-type' : 'text/plain',
            },
            body: queryText
        });
        if (!data.ok) {
            throw new Error(`Error during GET query : ${data.statusText}`);
        }
        return data.text();
    }
}

const llmService = new LLMService();
export default llmService;

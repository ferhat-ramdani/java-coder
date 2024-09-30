import Config from '../Config';
import {LLM} from "../interfaces/LLM";

class LLMService {

    private config: Config = Config.getInstance();
    private backendUrl: string;

    constructor() {
        this.backendUrl = this.config.getBackendUrl();
    }

    // async getLLMS(): Promise<LLM[]> {
    //     const response = await fetch(`${this.backendUrl}/api/llm`);
    //     if (!response.ok) {
    //         throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
    //     }
    //     return await response.json();
    // }

    async getLLMS(): Promise<LLM[]> {
        const response = await fetch(`${this.backendUrl}/api/llm`);
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return await response.json();
    }

    async getLlmById(id: number): Promise<LLM> {
        const response = await fetch(`${this.backendUrl}/api/llm/${id}`);
        if (!response.ok) {
            throw new Error(`Erreur lors de l'appel GET : ${response.statusText}`);
        }
        return await response.json();
    }
}

const llmService = new LLMService();
export default llmService;

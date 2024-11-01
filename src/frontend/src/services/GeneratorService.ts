import {createPrompt, Prompt} from "../interfaces/Prompt";
import Config from "../Config";
import {Utils} from "./Utils";
import {LLMResponse} from "../interfaces/LLMResponse";
import {AuthorType} from "../interfaces/AuthorType";

class GeneratorService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/gen`;

    async generateResponseFromLLM(prompt: Prompt, responseHandler: (llmResponse: LLMResponse, eventSource: EventSource, systemPrompt: Prompt, IndexOfPrompt: number) => number){

        await fetch(`${this.apiUrl}/class`, Utils.createRequestInit(prompt, 'POST'));
        const eventSource = new EventSource(`${this.apiUrl}/stream`)
        let index = -1;

        eventSource.onmessage = (event) => {
            const llmResponse: LLMResponse = JSON.parse(event.data);
            const systemPrompt: Prompt = createPrompt("", AuthorType.SYSTEM, prompt.chatId);
            index = responseHandler(llmResponse, eventSource, systemPrompt, index);
        }
    }

    async executeClass(id: number): Promise<string> {
        const response = await fetch(`${this.config.getBackendUrl()}/api/gen/exec`, Utils.createRequestInit(id, 'POST'));
        await Utils.showErrorToast(response, "Error during class execution.");
        return await response.text();
    }
}

const generatorService =  new GeneratorService();
export default generatorService;

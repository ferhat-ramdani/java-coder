import {Prompt} from "../interfaces/Prompt";
import Config from "../Config";
import {Utils} from "./Utils";
import {LLMResponse} from "../interfaces/LLMResponse";

class GeneratorService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/gen`;

    async generateResponseFromLLM(prompt: Prompt, onUpdate: (llmResponse: LLMResponse, eventSource: EventSource) => void) {
        const response = await fetch(`${this.apiUrl}/class`, Utils.createRequestInit(prompt, 'POST'));
        await Utils.showErrorToast(response, "Error during class registration.");
        const registeredPromptId = await response.text().then(id => parseInt(id)).catch(e => console.error(e));
        const eventSource = new EventSource(`${this.apiUrl}/stream/${registeredPromptId}`);

        eventSource.onmessage = (event) => {
            const llmResponse: LLMResponse = JSON.parse(event.data);
            onUpdate(llmResponse, eventSource);
        }
    }
}

const generatorService = new GeneratorService();
export default generatorService;

import {Prompt} from "../interfaces/Prompt";
import Config from "../Config";
import {Utils} from "./Utils";
import {ErrorResponse} from "../interfaces/ErrorResponse";

class GeneratorService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/gen`;

    async generateResponseFromLLM(prompt: Prompt): Promise<Prompt>{
        const data = await fetch(`${this.apiUrl}/class`, Utils.createRequestInit(prompt, 'POST'));

        await Utils.showErrorToast(data, "Error during class generation.");

        return data.json();
    }

    async executeClass(id: number): Promise<string> {
        const response = await fetch(`${this.config.getBackendUrl()}/api/gen/exec`, Utils.createRequestInit(id, 'POST'));

        await Utils.showErrorToast(response, "Error during class execution.");
        
        return await response.text();
    }
}

const generatorService =  new GeneratorService();
export default generatorService;

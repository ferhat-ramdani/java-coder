import {Prompt} from "../interfaces/Prompt";
import Config from "../Config";
import {Utils} from "./Utils";

class GeneratorService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/gen`;

    async generateResponseFromLLM(prompt: Prompt): Promise<Prompt>{
        const data = await fetch(`${this.apiUrl}/class`, Utils.createRequestInit(1, 'POST'));
        if (!data.ok) {
            throw new Error(`Error during GET query : ${data.statusText}`);
        }
        return data.json();
    }

    async executeClass(id : string): Promise<string>{
        const data = await fetch(`${this.config.getBackendUrl()}/api/gen/exec/`, Utils.createRequestInit(id, 'POST'));
        if (!data.ok) {
            throw new Error(`Error during GET query : ${data.statusText}`);
        }
        return data.text();
    }

}

const generatorService =  new GeneratorService();
export default generatorService;

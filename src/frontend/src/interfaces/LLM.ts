export interface LLM {
    id: number;
    name: string;
    model: string;
    systemPrompt: string;
    temp: number;
    seed: number;
    timeoutSec: number;
    installed: boolean;
}

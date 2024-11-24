import {Prompt} from "./Prompt";

export interface LLMResponse {
    status: LLMResponseStatus;
    content: string | null;
    prompt: Prompt | null;
}

export enum LLMResponseStatus {
    GENERATING = "GENERATING",
    ERROR = "ERROR",
    SUCCESS = "SUCCESS"
}
import {Prompt} from "./Prompt";

export interface SourceCode {
    code: string;
    compiled: boolean;
}

export interface LLMResponse {
    status: LLMResponseStatus;
    content: string | null;
    prompt: Prompt | null;
}

export enum LLMResponseStatus {
    GENERATING = "GENERATING",
    ERROR = "ERROR",
    TIMEOUT = "TIMEOUT",
    DONE = "DONE"
}
export interface SourceCode {
    code: string;
    compiled: boolean;
}

export interface LLMResponse {
    status: LLMResponseStatus;
    content: string | null;
    code: SourceCode | null;
}

export enum LLMResponseStatus {
    GENERATING = "GENERATING",
    ERROR = "ERROR",
    TIMEOUT = "TIMEOUT",
    DONE = "DONE"
}
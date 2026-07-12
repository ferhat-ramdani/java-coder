import {Prompt} from "./Prompt";

export enum LLMResponseStatus {
    PROGRESS = "PROGRESS",
    ERROR = "ERROR",
    SUCCESS = "SUCCESS"
}

export enum GenPhase {
    GENERATING = "GENERATING",
    COMPILING = "COMPILING",
    VALIDATING = "VALIDATING",
    RETRY = "RETRY",
    DONE = "DONE"
}

export interface LLMResponse {
    status: LLMResponseStatus;
    phase: GenPhase;
    attempt: number;
    maxAttempts: number;
    message: string | null;
    detail: string | null;
    prompt: Prompt | null;
}

export interface GenerationStep {
    phase: GenPhase;
    attempt: number;
    maxAttempts: number;
    message: string;
    detail: string | null;
    timestamp: number;
}

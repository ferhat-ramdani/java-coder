import {AuthorType} from "./AuthorType";
import {GenerationStep} from "./LLMResponse";

export interface Prompt {
    id : number;
    message: string;
    authorType: AuthorType;
    chatId: number;
    compile: boolean;
    temporary: boolean;
    /** Only set on the transient, non-persisted SYSTEM prompt shown while a generation is in flight. */
    generationHistory?: GenerationStep[];
}

export function createPrompt(message: string,
                             authorType: AuthorType,
                             chatId: number,
                             compile: boolean = false,
                             temporary: boolean = false): Prompt {
    return {
        id: 0,
        message: message.trim(),
        authorType: authorType,
        chatId: chatId,
        compile: compile,
        temporary: temporary
    };
}

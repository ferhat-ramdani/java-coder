import {AuthorType} from "./AuthorType";

export interface Prompt {
    id : number;
    message: string;
    authorType: AuthorType;
    chatId: number;
    compile: boolean;
    temporary: boolean;
}

export function createPrompt(message: string, authorType: AuthorType, chatId: number, compile: boolean = false, temporary: boolean = false): Prompt {
    return {
        id: 0,
        message: message.trim(),
        authorType: authorType,
        chatId: chatId,
        compile: compile,
        temporary: temporary
    };
}

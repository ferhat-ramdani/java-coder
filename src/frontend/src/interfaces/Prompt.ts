import {AuthorType} from "./AuthorType";

export interface Prompt {
    id : number;
    message: string;
    authorType: AuthorType;
    chatId: number;
    compile: boolean;
}

export function createPrompt(message: string, authorType: AuthorType, chatId: number, compile: boolean = false): Prompt {
    return {
        id: 0,
        message: message.trim(),
        authorType: authorType,
        chatId: chatId,
        compile: compile
    };
}

export function arePromptsEqual(prompt1: Prompt, prompt2: Prompt): boolean {
    return prompt1.id === prompt2.id && prompt1.message === prompt2.message && prompt1.authorType === prompt2.authorType && prompt1.chatId === prompt2.chatId;
}
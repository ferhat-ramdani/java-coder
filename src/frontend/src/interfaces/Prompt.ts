export interface Prompt {
    id : number;
    message: string;
    authorType: AuthorType;
    chatId: number;
}

export function createPromt(message: string, authorType: AuthorType, chatId: number): Prompt {
    return {
        id: 0,
        message: message.trim(),
        authorType: authorType,
        chatId: chatId,
    };
}

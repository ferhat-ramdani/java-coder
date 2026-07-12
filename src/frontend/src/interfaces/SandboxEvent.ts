export enum SandboxEventType {
    OUTPUT = "OUTPUT",
    EXITED = "EXITED",
    ERROR = "ERROR"
}

export interface SandboxEvent {
    type: SandboxEventType;
    data: string;
}

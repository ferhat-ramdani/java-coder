import Config from "../Config";
import {Utils} from "./Utils";
import {SandboxEvent} from "../interfaces/SandboxEvent";

class SandboxService {

    private config: Config = Config.getInstance();
    private apiUrl: string = `${this.config.getBackendUrl()}/api/sandbox`;

    async startSession(promptId: number): Promise<string> {
        const response = await fetch(`${this.apiUrl}/exec/${promptId}`, {method: 'POST'});
        await Utils.showErrorToast(response, "Error starting sandbox session.");
        const data: { sessionId: string } = await response.json();
        return data.sessionId;
    }

    streamSession(sessionId: string, onEvent: (event: SandboxEvent) => void, onError?: () => void): EventSource {
        const eventSource = new EventSource(`${this.apiUrl}/exec/${sessionId}/stream`);
        eventSource.onmessage = (e) => {
            onEvent(JSON.parse(e.data) as SandboxEvent);
        };
        eventSource.onerror = () => {
            onError?.();
        };
        return eventSource;
    }

    async sendInput(sessionId: string, input: string): Promise<void> {
        const response = await fetch(`${this.apiUrl}/exec/${sessionId}/input`, Utils.createRequestInit({input}, 'POST'));
        await Utils.showErrorToast(response, "Error sending input.");
    }

    async stopSession(sessionId: string): Promise<void> {
        await fetch(`${this.apiUrl}/exec/${sessionId}`, {method: 'DELETE'});
    }
}

const sandboxService = new SandboxService();
export default sandboxService;

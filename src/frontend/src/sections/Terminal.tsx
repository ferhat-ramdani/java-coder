import {Component, createSignal, onCleanup, Show} from "solid-js";
import sandboxService from "../services/SandboxService";
import {SandboxEvent, SandboxEventType} from "../interfaces/SandboxEvent";

interface TerminalProps {
    promptId: number;
    onClose: () => void;
}

type SessionStatus = "starting" | "running" | "exited" | "failed";

const STATUS_BADGE: Record<SessionStatus, string> = {
    starting: "text-bg-secondary",
    running: "text-bg-primary",
    exited: "text-bg-success",
    failed: "text-bg-danger",
};

const formatElapsed = (totalSeconds: number): string => {
    if (totalSeconds < 60) return `${totalSeconds}s`;
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}m ${seconds.toString().padStart(2, "0")}s`;
};

const Terminal: Component<TerminalProps> = (props) => {
    const [output, setOutput] = createSignal("");
    const [status, setStatus] = createSignal<SessionStatus>("starting");
    const [exitCode, setExitCode] = createSignal<number | null>(null);
    const [inputValue, setInputValue] = createSignal("");
    const [elapsed, setElapsed] = createSignal(0);

    let eventSource: EventSource | undefined;
    let sessionId: string | null = null;
    let bodyRef: HTMLPreElement | undefined;
    let inputRef: HTMLInputElement | undefined;
    let timer: number | undefined;

    const startTimer = () => {
        stopTimer();
        timer = window.setInterval(() => setElapsed(s => s + 1), 1000);
    };
    const stopTimer = () => {
        if (timer !== undefined) {
            window.clearInterval(timer);
            timer = undefined;
        }
    };

    const append = (text: string) => {
        setOutput(prev => prev + text);
        queueMicrotask(() => bodyRef?.scrollTo({top: bodyRef.scrollHeight}));
    };

    const handleEvent = (event: SandboxEvent) => {
        switch (event.type) {
            case SandboxEventType.OUTPUT:
                append(event.data);
                break;
            case SandboxEventType.EXITED: {
                const code = parseInt(event.data, 10);
                setExitCode(code);
                setStatus(code === 0 ? "exited" : "failed");
                stopTimer();
                append(`\n\n[process exited with code ${event.data} after ${formatElapsed(elapsed())}]`);
                eventSource?.close();
                break;
            }
            case SandboxEventType.ERROR:
                setStatus("failed");
                stopTimer();
                append(`\n\n[${event.data}]`);
                break;
        }
    };

    (async () => {
        try {
            sessionId = await sandboxService.startSession(props.promptId);
            setStatus("running");
            startTimer();
            eventSource = sandboxService.streamSession(sessionId, handleEvent, () => {
                if (status() === "running") {
                    setStatus("failed");
                    stopTimer();
                    append("\n\n[connection to the sandbox was lost]");
                }
            });
        } catch (e) {
            setStatus("failed");
            append(`[failed to start the sandbox: ${e}]`);
        }
    })();

    onCleanup(() => {
        stopTimer();
        eventSource?.close();
        if (sessionId && status() === "running") {
            sandboxService.stopSession(sessionId);
        }
    });

    const sendInput = async () => {
        const value = inputValue();
        if (!value || !sessionId || status() !== "running") return;
        append(`> ${value}\n`);
        setInputValue("");
        try {
            await sandboxService.sendInput(sessionId, value);
        } finally {
            inputRef?.focus();
        }
    };

    const stop = async () => {
        if (sessionId) await sandboxService.stopSession(sessionId);
        eventSource?.close();
        stopTimer();
        setStatus("exited");
    };

    const statusLabel = () => {
        switch (status()) {
            case "starting": return "Starting…";
            case "running": return "Running";
            case "exited": return `Exited (${exitCode() ?? 0})`;
            case "failed": return exitCode() !== null ? `Exited (${exitCode()})` : "Error";
        }
    };

    return (
        <div class="terminal-panel">
            <div class="terminal-toolbar">
                <span class="terminal-title"><i class="bi bi-terminal me-1"></i>Sandbox terminal</span>
                <span class={`badge rounded-pill ${STATUS_BADGE[status()]}`}>{statusLabel()}</span>
                <Show when={status() === "running" || status() === "exited" || status() === "failed"}>
                    <span class="terminal-elapsed"><i class="bi bi-stopwatch me-1"></i>{formatElapsed(elapsed())}</span>
                </Show>
                <div class="ms-auto d-flex gap-2">
                    <Show when={status() === "running"}>
                        <button class="btn btn-sm btn-outline-danger" onClick={stop} title="Stop execution">
                            <i class="bi bi-stop-fill"></i>
                        </button>
                    </Show>
                    <button class="btn btn-sm btn-outline-secondary" onClick={props.onClose} title="Close terminal">
                        <i class="bi bi-x-lg"></i>
                    </button>
                </div>
            </div>
            <pre ref={bodyRef} class="terminal-body modern-scroll">{output() || "Starting sandboxed container…"}</pre>
            <div class="terminal-input-row">
                <span class="terminal-prompt-caret">&gt;</span>
                <input
                    ref={inputRef}
                    type="text"
                    class="terminal-input"
                    placeholder={status() === "running" ? "Type input and press Enter…" : "Program is not running"}
                    disabled={status() !== "running"}
                    value={inputValue()}
                    onInput={(e) => setInputValue(e.currentTarget.value)}
                    onKeyDown={(e) => {
                        if (e.key === "Enter") sendInput();
                    }}
                />
            </div>
        </div>
    );
};

export default Terminal;

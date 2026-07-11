import {Component, createResource, createSignal, For, onMount, Show} from "solid-js";
import llmService from "../services/LLMService";
import {useAppContext} from "../Context";
import {Spinner} from "./Spinner";
import {LLM} from "../interfaces/LLM";

const fetchLLM = async () => await llmService.getLLMS();

const DEFAULT_SYSTEM_PROMPT = "You are an experienced Java developer. Respond to each request by providing a simple Java class with a main method. Provide only the code without any explanations.";

type DownloadState = {
    llmId: number;
    progress: number;
    stage: string;
    error: string | null;
    finished: boolean;
};

const parseProgressPercent = (line: string): number | null => {
    const match = line.match(/(\d{1,3})\s*%/);
    if (!match) return null;
    const pct = parseInt(match[1], 10);
    return pct >= 0 && pct <= 100 ? pct : null;
};

// Ollama's CLI output is meant for a live terminal: it's peppered with ANSI cursor/escape
// sequences and spinner glyphs that need to be stripped before showing it as plain text.
const sanitizeStage = (line: string): string => {
    return line
        .replace(/\x1b\[[0-9;?]*[a-zA-Z]/g, "")
        .replace(/[⠀-⣿]/g, "")
        .replace(/\s+/g, " ")
        .trim();
};

const LLMsUI: Component = () => {
    const [{pageTitle}] = useAppContext();
    const [llms, { refetch }] = createResource(fetchLLM);

    const handleLLMChange = (llmId: number) => {
        const llm = llms()?.find((llm) => llm.id === llmId);
        if (llm && llm.installed) {
            localStorage.setItem('default-llm', JSON.stringify(llm.id))
            setDefaultLLM(llm);
        }
    };

    const [defaultLLM, setDefaultLLM] = createSignal<LLM | null>(null);
    onMount(async () => {
        pageTitle.setter("List of LLMs");
        const storedLLMId = localStorage.getItem('default-llm');
        if (storedLLMId) {
            const llm = await llmService.getLlmById(JSON.parse(storedLLMId));
            setDefaultLLM(llm);
        }
        pageTitle.setter("List of LLMs");
    });

    const [newLLM, setNewLLM] = createSignal<Partial<LLM>>({
        name: "",
        model: "",
        systemPrompt: DEFAULT_SYSTEM_PROMPT,
        temp: 0,
        seed: 0,
        timeoutSec: 0
    });
    const [addError, setAddError] = createSignal("");
    const [download, setDownload] = createSignal<DownloadState | null>(null);

    const resetNewLLMForm = () => {
        setNewLLM({
            name: "",
            model: "",
            systemPrompt: DEFAULT_SYSTEM_PROMPT,
            temp: 0,
            seed: 0,
            timeoutSec: 0
        });
    };

    const startDownload = (llmId: number) => {
        setDownload({ llmId, progress: 0, stage: "Starting download...", error: null, finished: false });
        const eventSource = new EventSource(llmService.getPullUrl(llmId));

        eventSource.onmessage = (event) => {
            if (event.data === "DONE") {
                eventSource.close();
                setDownload(prev => prev && ({ ...prev, progress: 100, stage: "Installed successfully", finished: true }));
                refetch();
            } else if (event.data.startsWith("ERROR:")) {
                eventSource.close();
                setDownload(prev => prev && ({ ...prev, error: event.data.replace(/^ERROR:\s*/, ""), finished: true }));
            } else {
                const pct = parseProgressPercent(event.data);
                const stage = sanitizeStage(event.data);
                setDownload(prev => prev && ({
                    ...prev,
                    stage: stage || prev.stage,
                    progress: pct ?? prev.progress,
                }));
            }
        };

        eventSource.onerror = () => {
            eventSource.close();
            setDownload(prev => (prev && !prev.finished)
                ? { ...prev, error: "Connection to the server was lost during installation.", finished: true }
                : prev);
        };
    };

    const handleAddLLM = async (e: Event) => {
        e.preventDefault();
        setAddError("");
        try {
            const addedLlm = await llmService.addLLM(newLLM());
            resetNewLLMForm();
            await refetch();
            startDownload(addedLlm.id);
        } catch (error: any) {
            setAddError(error.message || "Failed to add model.");
        }
    };

    const isBusyDownloading = () => download() !== null && !download()?.finished;

    return (<>
        <div class="container">
            <div class="list-group my-3">
                <Show when={!llms.loading} fallback={<Spinner text={`Loading...`}/>}>
                    <For each={llms()} fallback={<div class="p-4 text-center text-muted">No models configured yet. Add one below.</div>}>
                        {(item) => (
                            <div class="list-group-item p-3">
                                <div class="d-flex align-items-center gap-3">
                                    <input class="form-check-input flex-shrink-0" type="radio"
                                           disabled={!item.installed}
                                           title={item.installed ? "Use as default model" : "Download this model first"}
                                           checked={defaultLLM()?.id === item.id}
                                           onChange={() => handleLLMChange(item.id)}/>
                                    <div class="flex-grow-1 hover-darken" style={item.installed ? "cursor: pointer" : ""} onClick={() => handleLLMChange(item.id)}>
                                        <div>
                                            <span class={`fw-bold`}>{item.name}</span>
                                            <Show when={defaultLLM()?.id === item.id}>
                                                <span class="badge text-bg-primary ms-2">Active</span>
                                            </Show>
                                        </div>
                                        <code class="text-muted small">{item.model}</code>
                                    </div>
                                    <Show when={item.installed} fallback={
                                        <button type="button" class="btn btn-sm btn-outline-primary flex-shrink-0"
                                                disabled={isBusyDownloading()}
                                                onClick={() => startDownload(item.id)}>
                                            <i class="bi bi-download me-1"></i>Download
                                        </button>
                                    }>
                                        <span class="badge text-bg-success flex-shrink-0"><i class="bi bi-check-circle-fill me-1"></i>Installed</span>
                                    </Show>
                                </div>
                                <Show when={download()?.llmId === item.id}>
                                    <div class="mt-2">
                                        <div class="progress" style="height: 18px;">
                                            <div class={`progress-bar ${download()?.error ? 'bg-danger' : 'progress-bar-striped progress-bar-animated'}`}
                                                 role="progressbar"
                                                 style={`width: ${download()?.progress ?? 0}%`}>
                                                {download()?.progress ?? 0}%
                                            </div>
                                        </div>
                                        <small class={`d-block mt-1 text-truncate ${download()?.error ? 'text-danger' : 'text-muted'}`}>
                                            {download()?.error || download()?.stage}
                                        </small>
                                    </div>
                                </Show>
                            </div>)}
                    </For>
                </Show>
            </div>

            <div class="card mt-4 mb-5">
                <div class="card-header">
                    Add New Local Model
                </div>
                <div class="card-body">
                    <form onSubmit={handleAddLLM}>
                        <div class="mb-3">
                            <label class="form-label">Name</label>
                            <input type="text" class="form-control" required value={newLLM().name || ""} onInput={(e) => setNewLLM({...newLLM(), name: e.currentTarget.value})} />
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Model ID (e.g., qwen2.5:0.5b)</label>
                            <input type="text" class="form-control" required value={newLLM().model || ""} onInput={(e) => setNewLLM({...newLLM(), model: e.currentTarget.value})} />
                            <div class="form-text">Browse available models at <a href="https://ollama.com/library" target="_blank" rel="noopener noreferrer">ollama.com/library</a>.</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">System Prompt</label>
                            <textarea class="form-control" rows={3} value={newLLM().systemPrompt || ""} onInput={(e) => setNewLLM({...newLLM(), systemPrompt: e.currentTarget.value})}></textarea>
                        </div>
                        <Show when={addError()}>
                            <div class="alert alert-danger py-2 d-flex align-items-center" role="alert">
                                <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
                                <small class="fw-semibold">{addError()}</small>
                            </div>
                        </Show>
                        <button type="submit" class="btn btn-primary" disabled={isBusyDownloading()}>
                            <i class="bi bi-plus-lg me-1"></i>Add &amp; Download Model
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </>);
};

export default LLMsUI;

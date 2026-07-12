import {Component, createResource, createSignal, For, onMount, Show} from "solid-js";
import llmService from "../services/LLMService";
import {useAppContext} from "../Context";
import {Spinner} from "./Spinner";
import {LLM} from "../interfaces/LLM";

const fetchLLM = async () => await llmService.getLLMS();

const LLMsUI: Component = () => {
    const [{pageTitle}] = useAppContext();
    const [llms, { mutate, refetch }] = createResource(fetchLLM);

    const handleLLMChange = (llmId: number) => {
        const llm = llms()?.find((llm) => llm.id === llmId);
        if (llm) {
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
        systemPrompt: "You are an experienced Java developer. Respond to each request by providing a simple Java class with a main method. Provide only the code without any explanations.",
        characteristics: "",
        temp: 0,
        seed: 0,
        timeoutSec: 0
    });
    const [isInstalling, setIsInstalling] = createSignal(false);

    const handleAddLLM = async (e: Event) => {
        e.preventDefault();
        setIsInstalling(true);
        try {
            await llmService.addLLM(newLLM());
            refetch();
            setNewLLM({
                name: "",
                model: "",
                systemPrompt: "You are an experienced Java developer. Respond to each request by providing a simple Java class with a main method. Provide only the code without any explanations.",
                characteristics: "",
                temp: 0,
                seed: 0,
                timeoutSec: 0
            });
        } finally {
            setIsInstalling(false);
        }
    };

    return (<>
        <div class="container">
            <div class="list-group my-3">
                <For each={llms()} fallback={<Spinner text={`Loading...`}/>}>
                    {(item) => (
                        <label class="list-group-item d-flex gap-2 hover-darken" onClick={() => handleLLMChange(item.id)}>
                            <input class="form-check-input flex-shrink-0" type="radio"
                                   checked={defaultLLM()?.id === item.id}/>
                            <div>
                                <span class={`fw-bold`}>{item.name}</span>
                                <For each={item.characteristics.split(";")}>
                                    {(characteristic) => <small class="d-block text-body-secondary">{characteristic.trim()}</small>}
                                </For>
                            </div>
                        </label>)}
                </For>
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
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Characteristics</label>
                            <input type="text" class="form-control" value={newLLM().characteristics || ""} onInput={(e) => setNewLLM({...newLLM(), characteristics: e.currentTarget.value})} />
                        </div>
                        <div class="mb-3">
                            <label class="form-label">System Prompt</label>
                            <textarea class="form-control" rows={3} value={newLLM().systemPrompt || ""} onInput={(e) => setNewLLM({...newLLM(), systemPrompt: e.currentTarget.value})}></textarea>
                        </div>
                        <button type="submit" class="btn btn-primary" disabled={isInstalling()}>
                            <Show when={isInstalling()} fallback="Install Model">
                                <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                Installing (this may take a while)...
                            </Show>
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </>);
};

export default LLMsUI;

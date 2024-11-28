import {Component, createEffect, createResource, createSignal, For, onMount} from "solid-js";
import llmService from "../services/LLMService";
import {useAppContext} from "../Context";
import {Spinner} from "./Spinner";
import {LLM} from "../interfaces/LLM";

const fetchLLM = async () => await llmService.getLLMS();

const LLMsUI: Component = () => {
    const [{pageTitle}] = useAppContext();
    const [llms] = createResource(fetchLLM);

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
        </div>
    </>);
};

export default LLMsUI;

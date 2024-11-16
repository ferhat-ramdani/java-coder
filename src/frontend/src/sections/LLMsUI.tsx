import {Component, createResource, For, onMount} from "solid-js";
import llmService from "../services/LLMService";
import {useAppContext} from "../Context";
import {Spinner} from "./Spinner";
import {useNavigate} from "@solidjs/router";

const fetchLLM = async () => await llmService.getLLMS();

const LLMsUI: Component = () => {
    const [{selectedLLM, pageTitle}] = useAppContext();
    const [llms] = createResource(fetchLLM);

    const navigate = useNavigate();
    const handleLLMChange = (llmId: number) => {
        const llm = llms()?.find((llm) => llm.id === llmId);
        if (llm) {
            selectedLLM.setter(llm);
        }
    };

    onMount(() => {
        if(selectedLLM.accessor() === null) {
            const firstLLM = llms()?.[0];
            if (firstLLM) {
                selectedLLM.setter(firstLLM);
            } else {
                navigate("/")
            }
        }
        pageTitle.setter("LLM's List");
    });

    return (<>
        <div class="container">
            <div class="list-group my-3">
                <For each={llms()} fallback={<Spinner text={`Loading...`}/>}>
                    {(item) => (
                        <label class="list-group-item d-flex gap-2 hover-darken" onClick={() => handleLLMChange(item.id)}>
                            <input class="form-check-input flex-shrink-0" type="radio"
                                   checked={selectedLLM.accessor()?.id === item.id}/>
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

import { Component, createResource, For } from "solid-js";
import llmService from "../services/LLMService";
import { LLM } from "../interfaces/LLM";

interface LLMMOdelSelectorProps {
    selectedLLM: () => LLM | null;
}

const fetchLLM = async () => await llmService.getLLMS();

const LLMModelSelector: Component<LLMMOdelSelectorProps> = (props) => {
    const [llms] = createResource(fetchLLM);

    return (
        <div class="position-fixed top-0 end-0 m-3">
            <select
                class="form-select"
                aria-label="Default select example"
                disabled={llms.loading}
                value={props.selectedLLM() ? props.selectedLLM()!.model : ""}
            >
                <option value="" disabled>{llms.loading ? "Loading.." : "Select LLM Model"}</option>
                <For each={llms()}>
                    {item => <option value={`${item.model}`}>{item.name}</option>}
                </For>
            </select>
        </div>
    );
};

export default LLMModelSelector;

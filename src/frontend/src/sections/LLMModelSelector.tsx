import { Component, createResource, For } from "solid-js";
import llmService from "../services/LLMService";
import { LLM } from "../interfaces/LLM";

interface LLMMOdelSelectorProps {
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm: LLM) => void;
}

const fetchLLM = async () => await llmService.getLLMS();

const LLMModelSelector: Component<LLMMOdelSelectorProps> = (props) => {
    const [llms] = createResource(fetchLLM);

    const handleLLMChange = (event: Event) => {
        const selectedModel = Number((event.target as HTMLSelectElement).value);
        const selectedLLM = llms()?.find(llm => llm.id === selectedModel);
        if (selectedLLM) {
            props.setSelectedLLM(selectedLLM);
        }
    };

    return (
        <div class="position-fixed top-0 end-0 m-3">
            <select
                class="form-select"
                aria-label="Select LLM Model"
                disabled={llms.loading}
                onChange={handleLLMChange}
            >
                <option value="" selected disabled>{llms.loading ? "Loading.." : "Select LLM Model"}</option>
                <For each={llms()}>
                    {item => (
                        <option value={item.id} selected={props.selectedLLM()?.id === item.id}>
                            {item.name}
                        </option>
                    )}
                </For>
            </select>
        </div>
    );
};

export default LLMModelSelector;

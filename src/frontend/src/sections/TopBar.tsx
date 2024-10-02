import { Component, createResource, For } from "solid-js";
import llmService from "../services/LLMService";
import { LLM } from "../interfaces/LLM";

interface LLMMOdelSelectorProps {
    selectedLLM: () => LLM | null;
    setSelectedLLM: (llm: LLM) => void;
    curChatId: () => number | null;
}

const fetchLLM = async () => await llmService.getLLMS();

const TopBar: Component<LLMMOdelSelectorProps> = (props) => {
    const [llms] = createResource(fetchLLM);

    const handleLLMChange = (event: Event) => {
        const selectedModel = Number((event.target as HTMLSelectElement).value);
        const selectedLLM = llms()?.find(llm => llm.id === selectedModel);
        if (selectedLLM) {
            props.setSelectedLLM(selectedLLM);
        }
    };

    return (
        <div class="d-flex bg-light border-bottom">
            <div class="p-2 flex-grow-1 align-self-center">
                <span class="ms-3 mb-0 h3">ClassGen</span>
            </div>
            <div class="p-2 align-self-center">
                <select
                    class="form-select"
                    aria-label="Select LLM Model"
                    disabled={llms.loading || props.curChatId() != null}
                    onChange={handleLLMChange}
                >
                    <option value="" selected={props.selectedLLM() === null}
                            disabled>{llms.loading ? "Loading.." : "Select LLM Model"}</option>
                    <For each={llms()}>
                        {item => (
                            <option value={item.id} selected={props.selectedLLM()?.id === item.id}
                                    data-bs-toggle="tooltip" data-bs-placement="top" title={item.caracteristics}>
                                {item.name}
                            </option>
                        )}
                    </For>
                </select>
            </div>
        </div>
    );
};

export default TopBar;

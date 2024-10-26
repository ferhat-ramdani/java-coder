import {Component, createResource, For, Match, Switch} from "solid-js";
import llmService from "../services/LLMService";
import {useAppContext} from "../Context";

const fetchLLM = async () => await llmService.getLLMS();

const TopBar: Component = () => {
    const [{curChatId, selectedLLM}] = useAppContext();
    const [llms] = createResource(fetchLLM);

    const handleLLMChange = (event: Event) => {
        const selectedModel = Number((event.target as HTMLSelectElement).value);
        const llm = llms()?.find(llm => llm.id === selectedModel);
        if (llm) {
            selectedLLM.setter(llm);
        }
    };

    return (<div class="d-flex bg-light border-bottom">
            <div class="p-2 flex-grow-1 align-self-center">
                <span class="ms-3 mb-0 h3">ChatGPT</span>
            </div>
            <div class="p-2 align-self-center">
                <select
                    class="form-select"
                    aria-label="Select LLM Model"
                    disabled={llms.loading || curChatId.accessor() != null}
                    onChange={handleLLMChange}
                >
                    <option value="" selected={selectedLLM.accessor() === null}
                            disabled>{llms.loading ? "Loading.." : "Select LLM Model"}</option>
                    <For each={llms()} fallback={<option>Loading...</option>}>
                        {item => (
                            <option value={item.id} selected={selectedLLM.accessor()?.id === item.id}
                                    data-bs-toggle="tooltip" data-bs-placement="top" title={item.characteristics}>
                                {item.name}
                            </option>
                        )}
                    </For>
                </select>
            </div>
    </div>);
};

export default TopBar;

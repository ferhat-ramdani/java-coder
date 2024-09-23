import {Component, createResource, createSignal, For, onMount} from "solid-js";
import llmService from "../sevices/LLMService";



const fetchUser = async () =>
    await llmService.getLLMS();
const LLMModelSelector: Component = () => {

    const [user] = createResource(fetchUser);
    return (
        <div class="position-fixed top-0 end-0 m-3">
            <select class="form-select" aria-label="Default select example">
                <option selected>{user.loading && "Loading..." || "Select LLM Model"}</option>
                <For each={user()}>
                    {item => <option value={`${item.model}`}>{item.name}</option>}
                </For>
            </select>

            {/*<div class="dropdown">
                <button class="btn btn-secondary dropdown-toggle" type="button" id="modelSelector"
                        data-bs-toggle="dropdown" aria-expanded="false">
                    {user.loading && "Loading..." || "Select LLM Model"}
                </button>
                <ul class="dropdown-menu" aria-labelledby="modelSelector">
                    <For each={user()}>
                        {item => <li><a class="dropdown-item" href="#">{item.name} - {item.model}</a></li>}
                    </For>
                </ul>
            </div>*/}
        </div>
    );
};

export default LLMModelSelector;

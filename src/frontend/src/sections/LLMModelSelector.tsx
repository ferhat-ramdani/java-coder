import { Component } from "solid-js";

const LLMModelSelector: Component = () => {
    return (
        <div class="position-fixed top-0 end-0 m-3">
            <div class="dropdown">
                <button class="btn btn-secondary dropdown-toggle" type="button" id="modelSelector" data-bs-toggle="dropdown" aria-expanded="false">
                    Select LLM Model
                </button>
                <ul class="dropdown-menu" aria-labelledby="modelSelector">
                    <li><a class="dropdown-item" href="#">Model A</a></li>
                    <li><a class="dropdown-item" href="#">Model B</a></li>
                    <li><a class="dropdown-item" href="#">Model C</a></li>
                </ul>
            </div>
        </div>
    );
};

export default LLMModelSelector;

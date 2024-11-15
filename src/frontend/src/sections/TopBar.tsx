import {Component, createSignal} from "solid-js";
import {A} from "@solidjs/router";

const TopBar: Component = () => {
    const [isNavCollapsed, setIsNavCollapsed] = createSignal(true);

    const toggleNavbar = () => {
        setIsNavCollapsed(!isNavCollapsed());
    };

    return (
        <>
            <nav class="navbar navbar-expand-sm navbar-light bg-light border-bottom">
                <div class="container-fluid">
                    <A href={`/`} class={`navbar-brand`}>LLM's Chat</A>
                    <button class="navbar-toggler" type="button"
                            data-bs-target="#navbarNav" aria-controls="navbarNav"
                            aria-label="Toggle navigation" onClick={toggleNavbar}>
                        <span class="navbar-toggler-icon"></span>
                    </button>
                    <div class={`navbar-collapse ${isNavCollapsed() ? "collapse" : "show"}`} id="navbarNav">
                        <ul class="navbar-nav ms-auto">
                            <li class="nav-item">
                                <A href={`/chats`} class={`nav-link`}>Chats</A>
                            </li>
                            <li class="nav-item">
                                <A href={`/llms`} class={`nav-link`}>LLMS</A>
                            </li>
                            <li class="nav-item">
                                <A href={`/openapi/ui`} class={`nav-link`}>OpenAPI</A>
                            </li>
                        </ul>
                    </div>
                </div>
            </nav>
        </>
    );
};

export default TopBar;

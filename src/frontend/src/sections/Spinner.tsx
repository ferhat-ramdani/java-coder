import {Component, Show} from "solid-js";

interface SpinnerProps {
    text: string;
}
const Spinner: Component<SpinnerProps> = (props) => {
    return (
        <div class="d-flex align-items-center justify-content-center vh-100">
            <Show when={props.text}>
                <strong class="me-5" role="status">{props.text}</strong>
            </Show>
            <div class="spinner-border text-success" role="status">
                <span class="visually-hidden">{ props.text ? props.text : "Loading..."}</span>
            </div>
        </div>
    );
}

const SpinnerSmall: Component<SpinnerProps> = (props) => {
    return (
        <div>
            <Show when={props.text} fallback={''}>
                <span class="me-2">{props.text}</span>
            </Show>
            <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        </div>
    );
}

export { Spinner, SpinnerSmall};


import {Component, Show} from "solid-js";


const Spinner: Component = (props) => {
    return (
        <div className="d-flex align-items-center justify-content-center vh-100">
            <Show when={props.text} fallback={''}>
                <strong class="me-5" role="status">{props.text}</strong>
            </Show>
            <div className="spinner-border text-success" role="status">
                <span className="visually-hidden">{ props.text ? props.text : "Loading..."}</span>
            </div>
        </div>
    );
}

const SpinnerSmall: Component = (props) => {
    return (
        <div>
            <Show when={props.text} fallback={''}>
                <span className="me-2">{props.text}</span>
            </Show>
            <span className="spinner-border spinner-border-sm" aria-hidden="true"></span>
        </div>
    );
}

export { Spinner, SpinnerSmall};


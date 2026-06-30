import {Accessor, Component, createEffect, createSignal, Match, onCleanup, Setter, Show, Switch} from "solid-js";
import {AuthorType} from "../interfaces/AuthorType";
import {Prompt} from "../interfaces/Prompt";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";
import hljs from 'highlight.js';

interface PromptProps {
    prompt: Prompt;
}

const getPromptStyles = (authorType: AuthorType) => {
    let alignmentClass = "";

    switch (authorType) {
        case AuthorType.USER:
            alignmentClass = "justify-content-end";
            break;
        case AuthorType.SYSTEM:
            alignmentClass = "justify-content-start";
            break;
        case AuthorType.AI:
            alignmentClass = "justify-content-start";
            break;
    }

    return alignmentClass;
};

const execute = async (id: number, setExecutionDisabled: Setter<boolean>,) => {
    setExecutionDisabled(true);
    const res = await generatorService.executeClass(id);
    Utils.showNoActionModal('Execution output', res);
    setExecutionDisabled(false);
}

const setupIntervals = (
    temporary: boolean,
    setSeconds: Setter<number>,
    seconds: Accessor<number>
) => {
    let interval: NodeJS.Timeout | null = null;
    if (temporary) {
        interval = setInterval(() => setSeconds(seconds() + 1), 1000);
    }
    return { interval };
};
const PromptMessage: Component<PromptProps> = (props) => {
    const alignmentClass = getPromptStyles(props.prompt.authorType);
    const classes = `text-break chat-max-width rounded border`;

    let codeRef: HTMLPreElement | undefined;

    createEffect(() => {
        if (codeRef) hljs.highlightElement(codeRef);
    });

    const [seconds, setSeconds] = createSignal(0);
    const { interval } = setupIntervals(props.prompt.temporary, setSeconds, seconds);

    const [executeDisabled, setExecuteDisabled] = createSignal(false);
    const [copied, setCopied] = createSignal(false);

    onCleanup(() => {
        if (interval) clearInterval(interval);
    });

    const handleCopy = () => {
        navigator.clipboard.writeText(`${props.prompt.message}`);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    }

    return (
        <div class={`d-flex flex-column mb-3 ${alignmentClass === "justify-content-end" ? "align-items-end" : "align-items-start"}`}>
            <Switch>
                <Match when={props.prompt.authorType === AuthorType.AI}>
                    <div class={`position-relative chat-max-width shadow-sm rounded`}>
                        <pre class={`text-break rounded border m-0`}>
                          <code ref={codeRef} class={`language-java p-3`}>{props.prompt.message}</code>
                        </pre>
                        <button class={`btn btn-sm position-absolute top-0 end-0 m-2 ${copied() ? 'btn-success' : 'btn-outline-secondary bg-white'}`} onClick={handleCopy} title="Copy code">
                            <i class={copied() ? "bi bi-check-lg" : "bi bi-clipboard"}></i>
                            <Show when={copied()}><span class="ms-1 small fw-bold">Copied!</span></Show>
                        </button>
                        <Show when={props.prompt.compile}>
                            <button class="btn btn-sm btn-success position-absolute bottom-0 end-0 m-2 fw-bold shadow-sm"
                                    onClick={() => execute(props.prompt.id, setExecuteDisabled)}
                                    disabled={executeDisabled()} title="Run Code">
                                <i class="bi bi-play-fill me-1"></i>Run
                            </button>
                        </Show>
                    </div>
                    <Show when={!props.prompt.temporary && !props.prompt.compile}>
                        <div class="alert alert-warning mt-2 mb-0 py-2 d-flex align-items-center chat-max-width shadow-sm" role="alert">
                            <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
                            <small class="fw-semibold">The generated code could not be fully compiled or tested.</small>
                        </div>
                    </Show>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.USER}>
                    <div class={`${classes} px-3 py-2 bg-primary text-white shadow-sm`}>
                    {props.prompt.message}
                    </div>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.SYSTEM}>
                    <div class={`${classes} px-3 py-2 d-inline-flex align-items-center bg-surface-hover text-muted-custom shadow-sm`}>
                        <Show when={props.prompt.temporary}>
                            <span class="spinner-border spinner-border-sm text-primary me-3" role="status" aria-hidden="true"></span>
                            <span class="badge bg-primary-subtle text-primary rounded-pill me-3 border border-primary-subtle">{seconds()}s</span>
                        </Show>
                        <p class="m-0 fw-medium">{props.prompt.message}</p>
                    </div>
                </Match>
            </Switch>
        </div>
    );
};

export default PromptMessage;

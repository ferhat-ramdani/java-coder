import {Accessor, Component, createEffect, createSignal, For, Match, onCleanup, Setter, Show, Switch} from "solid-js";
import {AuthorType} from "../interfaces/AuthorType";
import {Prompt} from "../interfaces/Prompt";
import {GenPhase, GenerationStep} from "../interfaces/LLMResponse";
import Terminal from "./Terminal";
import hljs from 'highlight.js';

interface PromptProps {
    prompt: Prompt;
}

const getAlignmentClass = (authorType: AuthorType) =>
    authorType === AuthorType.USER ? "align-items-end" : "align-items-start";

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

const PHASE_ICON: Record<GenPhase, string> = {
    [GenPhase.GENERATING]: "bi-stars",
    [GenPhase.COMPILING]: "bi-gear-wide-connected",
    [GenPhase.VALIDATING]: "bi-play-circle",
    [GenPhase.RETRY]: "bi-arrow-repeat",
    [GenPhase.DONE]: "bi-check-circle",
};

/**
 * One entry in the generation timeline. Only the most recent entry auto-expands its detail (a
 * compiler error, a stack trace, ...); older entries stay collapsed to a single line so the
 * timeline reads as a story instead of a wall of text, but their detail is still one click away.
 */
const TimelineStep: Component<{ step: GenerationStep; isLatest: boolean }> = (props) => (
    <li class={`generation-step ${props.step.phase === GenPhase.RETRY ? "is-retry" : ""}`}>
        <i class={`bi ${PHASE_ICON[props.step.phase]} generation-step-icon ${props.step.phase === GenPhase.RETRY ? "text-warning" : "text-primary"}`}></i>
        <div class="flex-grow-1 min-w-0">
            <span class="generation-step-message">{props.step.message}</span>
            <Show when={props.step.detail}>
                <Show when={props.isLatest} fallback={
                    <details class="generation-step-detail">
                        <summary>View details</summary>
                        <pre>{props.step.detail}</pre>
                    </details>
                }>
                    <pre class="generation-step-detail-open">{props.step.detail}</pre>
                </Show>
            </Show>
        </div>
    </li>
);

/** The full step-by-step journey of a generation, always visible (never hidden behind a click). */
const GenerationTimeline: Component<{ history: GenerationStep[] }> = (props) => (
    <Show when={props.history.length > 0}>
        <ul class="generation-timeline">
            <For each={props.history}>
                {(step, i) => <TimelineStep step={step} isLatest={i() === props.history.length - 1}/>}
            </For>
        </ul>
    </Show>
);

const PromptMessage: Component<PromptProps> = (props) => {
    const alignmentClass = getAlignmentClass(props.prompt.authorType);

    let codeRef: HTMLElement | undefined;

    createEffect(() => {
        if (codeRef) hljs.highlightElement(codeRef);
    });

    const [seconds, setSeconds] = createSignal(0);
    const { interval } = setupIntervals(props.prompt.temporary, setSeconds, seconds);

    const [copied, setCopied] = createSignal(false);
    const [showTerminal, setShowTerminal] = createSignal(false);

    onCleanup(() => {
        if (interval) clearInterval(interval);
    });

    const handleCopy = () => {
        navigator.clipboard.writeText(`${props.prompt.message}`);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const history = () => props.prompt.generationHistory ?? [];
    const latestStep = () => history()[history().length - 1];
    // Once attempts are exhausted, the last emitted step IS the failing attempt's own detail
    // (compiler error / exception) - surface it directly instead of a generic message.
    const lastFailure = () => history().length > 0 ? history()[history().length - 1] : undefined;

    return (
        <div class={`d-flex flex-column mb-3 ${alignmentClass}`}>
            <Switch>
                <Match when={props.prompt.authorType === AuthorType.AI}>
                    <div class="position-relative code-bubble shadow-sm rounded">
                        <pre class="code-block rounded border m-0">
                          <code ref={codeRef} class="language-java p-3">{props.prompt.message}</code>
                        </pre>
                        <button class={`btn btn-sm position-absolute top-0 end-0 m-2 ${copied() ? 'btn-success' : 'btn-outline-secondary bg-white'}`} onClick={handleCopy} title="Copy code">
                            <i class={copied() ? "bi bi-check-lg" : "bi bi-clipboard"}></i>
                            <Show when={copied()}><span class="ms-1 small fw-bold">Copied!</span></Show>
                        </button>
                        <Show when={props.prompt.compile}>
                            <button class="btn btn-sm btn-success position-absolute bottom-0 end-0 m-2 fw-bold shadow-sm"
                                    onClick={() => setShowTerminal(!showTerminal())} title="Run in an isolated sandbox">
                                <i class={`bi ${showTerminal() ? "bi-x-lg" : "bi-play-fill"} me-1`}></i>{showTerminal() ? "Close" : "Run"}
                            </button>
                        </Show>
                    </div>
                    <Show when={showTerminal()}>
                        <div class="chat-bubble-wide mt-2">
                            <Terminal promptId={props.prompt.id} onClose={() => setShowTerminal(false)} />
                        </div>
                    </Show>
                    <Show when={!props.prompt.temporary}>
                        <div class="chat-bubble-wide mt-2">
                            <Show when={!props.prompt.compile}>
                                <div class="alert alert-warning py-2 mb-2 shadow-sm" role="alert">
                                    <div class="d-flex align-items-center">
                                        <i class="bi bi-exclamation-triangle-fill me-2 fs-5 flex-shrink-0"></i>
                                        <small class="fw-semibold">{lastFailure()?.message ?? "This code could not be verified and may not run correctly."}</small>
                                    </div>
                                    <Show when={lastFailure()?.detail}>
                                        <pre class="generation-step-detail-open mt-2 mb-0">{lastFailure()!.detail}</pre>
                                    </Show>
                                </div>
                            </Show>
                            <Show when={history().length > 0}>
                                <details class="generation-history" open>
                                    <summary>{props.prompt.compile ? "Generation steps" : "Full generation history"} ({history().length})</summary>
                                    <GenerationTimeline history={history()}/>
                                </details>
                            </Show>
                        </div>
                    </Show>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.USER}>
                    <div class="chat-bubble chat-bubble-user shadow-sm">
                        {props.prompt.message}
                    </div>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.SYSTEM}>
                    <div class={`chat-bubble ${props.prompt.temporary ? "chat-bubble-system" : "chat-bubble-system-error chat-bubble-wide"} shadow-sm`}>
                        <Show when={props.prompt.temporary} fallback={
                            <>
                                <div class="d-flex align-items-center">
                                    <i class="bi bi-exclamation-octagon-fill me-2 fs-5 flex-shrink-0 text-danger"></i>
                                    <p class="m-0 fw-medium">{props.prompt.message}</p>
                                </div>
                                <Show when={history().length > 0}>
                                    <details class="generation-history mt-2" open>
                                        <summary>What happened</summary>
                                        <GenerationTimeline history={history()}/>
                                    </details>
                                </Show>
                            </>
                        }>
                            <Show when={latestStep()} keyed fallback={
                                <div class="generation-live-header">
                                    <span class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></span>
                                    <span class="fw-medium flex-grow-1">Working...</span>
                                    <span class="badge bg-primary-subtle text-primary rounded-pill border border-primary-subtle">{seconds()}s</span>
                                </div>
                            }>
                                {(step) => (
                                    <div class="generation-live-header">
                                        <span class="spinner-border spinner-border-sm text-primary" role="status" aria-hidden="true"></span>
                                        <span class="fw-medium flex-grow-1">{step.message}</span>
                                        <span class="badge bg-primary-subtle text-primary rounded-pill border border-primary-subtle">{seconds()}s</span>
                                    </div>
                                )}
                            </Show>
                            <GenerationTimeline history={history()}/>
                        </Show>
                    </div>
                </Match>
            </Switch>
        </div>
    );
};

export default PromptMessage;

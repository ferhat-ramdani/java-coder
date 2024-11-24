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
    seconds: Accessor<number>,
    setDots: Setter<string>,
    dots: Accessor<string>
) => {
    let interval: NodeJS.Timeout | null = null;
    let dotsInterval: NodeJS.Timeout | null = null;
    if (temporary) {
        interval = setInterval(() => setSeconds(seconds() + 1), 1000);
    }
    if (temporary) {
        dotsInterval = setInterval(() => {
            setDots(dots().length < 3 ? dots() + "." : ".");
        }, 500);
    }
    return { interval, dotsInterval };
};
const PromptMessage: Component<PromptProps> = (props) => {
    const alignmentClass = getPromptStyles(props.prompt.authorType);
    const classes = `text-break max-width70 rounded border`;

    let codeRef: HTMLPreElement | undefined;

    createEffect(() => {
        if (codeRef) hljs.highlightElement(codeRef);
    });

    const [seconds, setSeconds] = createSignal(0);
    const [dots, setDots] = createSignal(".");
    const { interval, dotsInterval } = setupIntervals(props.prompt.temporary, setSeconds, seconds, setDots, dots);

    const [executeDisabled, setExecuteDisabled] = createSignal(false);
    onCleanup(() => {
        if (interval) clearInterval(interval);
        if (dotsInterval) clearInterval(dotsInterval);
    });

    return (
        <div class={`d-flex mb-2 ${alignmentClass}`}>
            <Switch>
                <Match when={props.prompt.authorType === AuthorType.AI}>
                    <div class={`position-relative max-width70`}>
                        <pre class={`text-break rounded border mx-2 my-1`}>
                          <code ref={codeRef} class={`language-java`}>{props.prompt.message}</code>
                        </pre>
                        <button class="btn btn-sm btn-outline-secondary position-absolute top-0 end-0 m-2" onClick={() => navigator.clipboard.writeText(`${props.prompt.message}`)}>
                            <i class="bi bi-clipboard-plus-fill"></i>
                        </button>
                        <Show when={props.prompt.compile}>
                            <button class="btn btn-sm btn-outline-primary position-absolute bottom-0 end-0 m-2"
                                    onClick={() => execute(props.prompt.id, setExecuteDisabled)}
                                    disabled={executeDisabled()}>
                                <i class="bi bi-power"></i>
                            </button>
                        </Show>
                    </div>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.USER}>
                    <div class={`${classes} px-2 py-1 bg-secondary-subtle`}>
                    {props.prompt.message}
                    </div>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.SYSTEM}>
                    <div class={`${classes} px-2 py-1 d-inline-flex align-items-center `}>
                        {!props.prompt.temporary ? null : (
                            <span class="badge text-bg-secondary ms-2">{seconds()}s</span>
                        )}
                        <p class="m-0 ms-2">{props.prompt.message}{!props.prompt.temporary ? "" : dots()}</p>
                    </div>
                </Match>
            </Switch>
        </div>
    );
};

export default PromptMessage;

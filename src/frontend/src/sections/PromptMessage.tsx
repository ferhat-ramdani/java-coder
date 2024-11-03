import {Accessor, Component, createSignal, Match, onCleanup, Setter, Show, Switch} from "solid-js";
import {AuthorType} from "../interfaces/AuthorType";
import {Prompt} from "../interfaces/Prompt";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";

interface PromptProps {
    prompt: Prompt;
}

const getPromptStyles = (authorType: AuthorType) => {
    let bgColor = "";
    let alignmentClass = "";

    switch (authorType) {
        case AuthorType.USER:
            bgColor = "text-bg-primary";
            alignmentClass = "align-items-end";
            break;
        case AuthorType.SYSTEM:
            bgColor = "text-bg-secondary";
            alignmentClass = "align-items-start";
            break;
        case AuthorType.AI:
            bgColor = "text-bg-success";
            alignmentClass = "align-items-start";
            break;
    }

    return { bgColor, alignmentClass };
};

const execute = async (id: number) => {
    const res = await generatorService.executeClass(id);
    Utils.showNoActionModal('Execution output', res);
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
    const { bgColor, alignmentClass } = getPromptStyles(props.prompt.authorType);
    const classes = `p-3 mt-2 mb-1 rounded-1 ${bgColor} text-start inline-block mw-100`;

    const [seconds, setSeconds] = createSignal(0);
    const [dots, setDots] = createSignal(".");
    const { interval, dotsInterval } = setupIntervals(props.prompt.temporary, setSeconds, seconds, setDots, dots);
    onCleanup(() => {
        if (interval) clearInterval(interval);
        if (dotsInterval) clearInterval(dotsInterval);
    });

    return (
        <div class={`d-flex flex-column ${alignmentClass}`}>
            <Switch>
                <Match when={props.prompt.authorType === AuthorType.AI}>
                    <pre class={classes}>
                      <code>{props.prompt.message}</code>
                    </pre>
                    <Show when={props.prompt.compile}>
                        <button class="btn btn-primary mt-2" onClick={_ => execute(props.prompt.id)}>
                            <i class="bi bi-power"></i>
                        </button>
                    </Show>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.USER}>
                    <div class={classes}>
                        <p class="m-0">{props.prompt.message}</p>
                    </div>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.SYSTEM}>
                    <div class={`${classes} d-inline-flex align-items-center`}>
                        {!props.prompt.temporary ? null : (
                            <span class="badge bg-dark ms-2">{seconds()}s</span>
                        )}
                        <p class="m-0 ms-2">{props.prompt.message}{!props.prompt.temporary ? "" : dots()}</p>
                    </div>
                </Match>
            </Switch>
        </div>
    );
};

export default PromptMessage;

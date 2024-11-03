import {Component, createSignal, Match, onCleanup, Show, Switch} from "solid-js";
import {AuthorType} from "../interfaces/AuthorType";
import {Prompt} from "../interfaces/Prompt";
import generatorService from "../services/GeneratorService";
import {Utils} from "../services/Utils";

interface PromptProps {
    prompt: Prompt;
}

const PromptMessage: Component<PromptProps> = (props) => {
    let bgColor = "";
    let alignmentClass = "";

    switch (props.prompt.authorType) {
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
    const classes = `p-3 mt-2 mb-1 rounded-1 ${bgColor} text-start inline-block mw-100`;

    const execute = async (id: number) => {
        const res = await generatorService.executeClass(id);
        Utils.showNoActionModal('Execution output', res);
    }

    const [secondsElapsed, setSecondsElapsed] = createSignal(0);
    const [dots, setDots] = createSignal(".");
    let interval: NodeJS.Timeout | null = null;
    if (props.prompt.temporary) {
        console.log("Prompt is temporary");
        console.log(props.prompt)
        console.log(props.prompt.temporary)
        interval = setInterval(() => setSecondsElapsed(secondsElapsed() + 1), 1000);
    }
    let dotsInterval: NodeJS.Timeout | null = null;
    if (props.prompt.temporary) {
        dotsInterval = setInterval(() => {
            setDots(dots().length < 3 ? dots() + "." : ".");
        }, 500);
    }
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
                        <button class="btn btn-primary mt-2" onClick={_ => execute(props.prompt.id)}><i class="bi bi-power"></i></button>
                    </Show>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.USER}>
                    <div class={classes}>
                        <p class="m-0">{props.prompt.message}</p>
                    </div>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.SYSTEM}>
                    <div class={classes}>
                        <p class="m-0">
                            {props.prompt.message}
                            {!props.prompt.temporary ? "" : dots()}
                        </p>
                        {!props.prompt.temporary ? null : (
                            <span class="badge bg-dark ms-2">{secondsElapsed()}s</span>
                        )}
                    </div>
                </Match>
            </Switch>
        </div>

    );
};

export default PromptMessage;

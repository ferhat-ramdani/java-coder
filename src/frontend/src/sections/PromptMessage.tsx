import {Component, Match, Show, Switch} from "solid-js";
import {AuthorType} from "../interfaces/AuthorType";
import {Prompt} from "../interfaces/Prompt";

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
        case AuthorType.LLM:
            bgColor = "text-bg-success";
            alignmentClass = "align-items-start";
            break;
    }
    const classes = `p-3 mt-2 mb-1 rounded-1 ${bgColor} text-start inline-block mw-100`;

    console.log(props.prompt);

    return (
        <div class={`d-flex flex-column ${alignmentClass}`}>
            <Switch>
                <Match when={props.prompt.authorType === AuthorType.LLM}>
                    <pre class={classes}>
                      <code>{props.prompt.message}</code>
                    </pre>
                    <Show when={props.prompt.compile}>
                        <button class="btn btn-primary mt-2"><i class="bi bi-power"></i></button>
                    </Show>
                </Match>
                <Match when={props.prompt.authorType === AuthorType.USER}>
                    <div class={classes}>
                        <p class="m-0">{props.prompt.message}</p>
                    </div>
                </Match>
            </Switch>
        </div>

    );
};

export default PromptMessage;

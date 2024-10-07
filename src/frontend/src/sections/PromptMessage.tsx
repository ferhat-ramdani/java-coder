import {Component, Match, Switch} from "solid-js";
import {AuthorType} from "../interfaces/AuthorType";

interface PromptProps {
    type: AuthorType;
    message: string;
}

const PromptMessage: Component<PromptProps> = (props) => {
    let bgColor = "";
    let alignmentClass = "";

    switch (props.type) {
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

    return (
        <div class={`d-flex flex-column ${alignmentClass}`}>
            <Switch>
                <Match when={props.type === AuthorType.LLM}>
                    <pre class={classes}>
                      <code>{props.message}</code>
                    </pre>
                </Match>
                <Match when={props.type === AuthorType.USER}>
                    <div class={classes}>
                        <p class="m-0">{props.message}</p>
                    </div>
                </Match>
            </Switch>
        </div>

    );
};

export default PromptMessage;

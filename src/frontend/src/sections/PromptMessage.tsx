import { Component } from "solid-js";

interface PromptProps {
    type: "user" | "system" | "llm";
    message: string;
}

const PromptMessage: Component<PromptProps> = (props) => {
    let bgColor = "";
    let alignmentClass = "";

    switch (props.type) {
        case "user":
            bgColor = "bg-primary text-white";
            alignmentClass = "align-items-end";
            break;
        case "system":
            bgColor = "bg-secondary text-white";
            alignmentClass = "align-items-end";
            break;
        case "llm":
            bgColor = "bg-success text-white";
            alignmentClass = "align-items-start";
            break;
    }

    const promptContent = props.type === "llm" ? (
        <pre class={`p-3 mt-2 mb-1 rounded ${bgColor} text-start`} style="display: inline-block; max-width: 80%;">
      <code>{props.message}</code>
    </pre>
    ) : (
        <div class={`p-3 mt-2 mb-1  rounded ${bgColor} text-start`} style="display: inline-block; max-width: 80%;">
            <p class="m-0">{props.message}</p>
        </div>
    );

    return (
        <div class={`d-flex flex-column ${alignmentClass}`}>
            {promptContent}
        </div>
    );
};

export default PromptMessage;

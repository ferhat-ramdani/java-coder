import { Component } from "solid-js";
import Prompt from "./Prompt";  // Import your Prompt component

const PromptsContainer: Component = () => {
    return (
        <div class="prompt-container w-50 flex-grow-1 overflow-auto">
            <Prompt type="user" message="Hi, write a class about monkeys" />
            <Prompt type="llm" message={`static void routing(HttpRouting.Builder routing) {
    routing.register("/greet", new GreetService()).register("/db", new DbService())
            .get("/simple-greet", (req, res) -> res.send("Hello World!"));
    registerFrontEndRoutes(routing);
}`} />
            <Prompt type="system" message="Not sure this is what I was looking for ..." />
        </div>
    );
};

export default PromptsContainer;

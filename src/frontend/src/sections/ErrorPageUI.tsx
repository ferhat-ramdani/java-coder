import {Component, onMount} from "solid-js";
import {useAppContext} from "../Context";
import {A} from "@solidjs/router";

const ErrorPageUI: Component = () => {

    const [{pageTitle}] = useAppContext();

    onMount(() => {
        pageTitle.setter("Page not found");
    });

    return (
        <>
            <div class="container-fluid d-flex flex-column flex-grow-1 overflow-hidden">
                <div class="flex-grow-1 overflow-auto p-3 border rounded my-3 d-flex flex-column align-items-center justify-content-center">
                    <h1 class={`text-center`}>404 - Page not found</h1>
                    <div class="text-center">
                        <p>The page you are looking for does not exist.</p>
                        <p>Please check the URL and try again.</p>
                    </div>
                    <A href={`/`} class={`btn btn-primary mt-3`}>Go to Home</A>
                </div>
            </div>
        </>
    );
}

export default ErrorPageUI;

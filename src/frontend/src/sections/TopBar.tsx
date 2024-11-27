import {Component, createEffect, createSignal} from "solid-js";
import {A} from "@solidjs/router";
import {useAppContext} from "../Context";
import {Link, MetaProvider, Title} from "@solidjs/meta";

const TopBar: Component = () => {

    const THEME_KEY = "user-theme";


    const [isNavCollapsed, setIsNavCollapsed] = createSignal(true);
    const [isDarkMode, setIsDarkMode] = createSignal(false);
    const [{pageTitle}] = useAppContext();

    const toggleNavbar = () => {
        setIsNavCollapsed(!isNavCollapsed());
    };

    const setDarkTheme = (prefersDark: boolean): void => {
        setIsDarkMode(prefersDark);
        localStorage.setItem(THEME_KEY, prefersDark ? "dark" : "light");
        document.documentElement.setAttribute("data-bs-theme", prefersDark ? "dark" : "light");
    }

    const loadTheme = (): void => {
        const savedTheme = localStorage.getItem(THEME_KEY);
        if (savedTheme === "dark") {
            setDarkTheme(true);
        } else if (savedTheme === "light") {
            setDarkTheme(false);
        } else {
            detectTheme();
        }
    };

    const detectTheme = (): void => {
        const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        setDarkTheme(prefersDark);
    };

    createEffect(() => {
        loadTheme();
    });

    const toggleTheme = (): void => {
        setDarkTheme(!isDarkMode());
    };

    return (
        <>
            <MetaProvider>
                <Title>{pageTitle.accessor()}</Title>
                <Link rel="stylesheet" href={`https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/vs${isDarkMode() ? "2015" : ""}.min.css`}/>
            </MetaProvider>
            <nav class="navbar navbar-expand-sm border-bottom">
                <div class="container-fluid">
                <A href={`/`} class={`navbar-brand`}>Java Coder</A>
                    <button class="navbar-toggler" type="button"
                            data-bs-target="#navbarNav" aria-controls="navbarNav"
                            aria-label="Toggle navigation" onClick={toggleNavbar}>
                        <span class="navbar-toggler-icon"></span>
                    </button>
                    <h5 class={`text-center w-100 m-0 text-truncate`}>{pageTitle.accessor()}</h5>
                    <div class={`navbar-collapse ${isNavCollapsed() ? "collapse" : "show"}`} id="navbarNav">
                        <ul class="navbar-nav ms-auto nav-underline">
                            <li class="nav-item">
                                <A href={`/chats`} class={`nav-link fs-5`} activeClass={`active`}>Chats</A>
                            </li>
                            <li class="nav-item">
                                <A href={`/llms`} class={`nav-link fs-5`} activeClass={`active`}>LLMS</A>
                            </li>
                        </ul>
                        <button class={`btn btn-sm ms-2 ${isDarkMode() ? 'btn-outline-warning' : 'btn-outline-secondary'}`} onClick={toggleTheme}>
                            {isDarkMode() ? <i class="bi bi-brightness-high-fill"></i> : <i class="bi bi-moon-fill"></i>}
                        </button>
                    </div>
                </div>
            </nav>
        </>
    );
};

export default TopBar;

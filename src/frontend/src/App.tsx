import type { Component } from 'solid-js';

import logo from './logo.svg';
import styles from './App.module.css';
import {Route, Router} from "@solidjs/router";
import Home from "./pages/Home";
import About from "./pages/About";

const App: Component = () => {
  return (
    <div class={styles.App}>
      <header class={styles.header}>
        <img src={logo} class={styles.logo} alt="logo" />
        <p>
          Edit <code>src/App.tsx</code> and save to reload.
        </p>
        <Router>
          <Route path="/" component={Home}/>
          <Route path="/about" component={About}/>
        </Router>

        <a
          class={styles.link}
          href="https://github.com/solidjs/solid"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn Solid
        </a>
      </header>
    </div>
  );
};

export default App;

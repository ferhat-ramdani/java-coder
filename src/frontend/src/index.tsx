/* @refresh reload */
import { render } from 'solid-js/web';
import {Route, Router} from "@solidjs/router";

import './index.css';
import "./styles.css";

import App from './App';

import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import 'bootstrap-icons/font/bootstrap-icons.css';
import {ContextProvider} from "./Context";
import TopBar from "./sections/TopBar";
import Sidebar from "./sections/Sidebar";
import PromptsContainer from "./sections/PromptsContainer";
import TextInput from "./sections/TextInput";
import {Suspense} from "solid-js";
import {Spinner} from "./sections/Spinner";

const wrapper = document.getElementById('root');

if (import.meta.env.DEV && !(wrapper instanceof HTMLElement)) {
  throw new Error(
    'Root element not found. Did you forget to add it to your index.html? Or maybe the id attribute got misspelled?',
  );
}

const Layout = (props: { children: any; }) => {
  return (
      <>
          <Suspense fallback={<Spinner text="Loading..."/>}>
              <div class="container-fluid vh-100">
                  <div class="row h-100">
                      <ContextProvider>
                          <div class="col d-flex flex-column p-0 h-100">
                              <TopBar/>
                              {props.children}
                          </div>
                      </ContextProvider>
                  </div>
              </div>
          </Suspense>
      </>
  );
};

render(() => (
    <Router root={Layout}>
        <Route path="/" component={App}/>
        <Route path="/hello-world" component={() => <div>Hello world!</div>}/>
    </Router>
), wrapper!);

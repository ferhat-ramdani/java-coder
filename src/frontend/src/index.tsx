/* @refresh reload */
import { render } from 'solid-js/web';
import {Route, Router} from "@solidjs/router";

import './index.css';
import "./styles.css";


import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import 'bootstrap-icons/font/bootstrap-icons.css';
import {ContextProvider} from "./Context";
import TopBar from "./sections/TopBar";
import ChatsUI from "./sections/ChatsUI";
import {Suspense} from "solid-js";
import {Spinner} from "./sections/Spinner";
import ChatUI from "./sections/ChatUI";
import ErrorPage from "./sections/ErrorPage";

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
              <ContextProvider>
                  <TopBar/>
                  {props.children}
              </ContextProvider>
          </Suspense>
      </>
  );
};

const filters = {
    id: /^\d+$/
};

render(() => (
    <Router root={Layout}>
        <Route path="/" component={ChatsUI}/>
        <Route path={["/chats", "/chat"]}>
            <Route path="/" component={ChatsUI}/>
            <Route path="/:id" component={ChatUI} matchFilters={filters}/>
        </Route>
        <Route path={`/llms`} component={}/>
        <Route path={`*404`} component={ErrorPage}/>
    </Router>
), wrapper!);

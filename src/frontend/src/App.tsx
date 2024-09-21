import type { Component } from 'solid-js';
import Prompt from "./pages/Prompt";

import 'bootstrap/dist/css/bootstrap.min.css';

const App: Component = () => {
  return (
      <div class="position-fixed top-0 start-0 end-0 d-flex flex-column align-items-center mb-3"
           style="height: 100vh; z-index: 1;">
        <div class="prompt-container w-50 flex-grow-1 overflow-auto">
          <Prompt type="user" message="Hi, write a class about monkeys"/>
          <Prompt type="llm" message="class Monkeys {}"/>
          <Prompt type="system" message="Not sure this is what I was looking for ..."/>
        </div>
        <div class="input-group w-50 mb-2">
        <textarea
            class="form-control rounded-start"
            rows="1"
            placeholder="Type your message here..."
            style="resize: none;"
        ></textarea>
          <button class="btn btn-primary rounded-end" type="button">
            Send
          </button>
        </div>
      </div>
  );
};

export default App;

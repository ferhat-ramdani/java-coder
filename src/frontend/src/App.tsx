import type { Component } from 'solid-js';

import 'bootstrap/dist/css/bootstrap.min.css';
import Sidebar from "./sections/Sidebar";
import TextInput from "./sections/TextInput";
import PromptsContainer from "./sections/PromptsContainer";
import LLMModelSelector from "./sections/LLMModelSelector";

const App: Component = () => {
  return (
      <div class="d-flex" style="height: 100vh;">
        <Sidebar/>
        <LLMModelSelector />
        <div class="flex-grow-1 d-flex flex-column align-items-center justify-content-between">
          <PromptsContainer/>
          <TextInput/>
        </div>
      </div>
  );
};

export default App;

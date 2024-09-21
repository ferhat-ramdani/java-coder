import { Component } from "solid-js";

const TextInput: Component = () => {
  return (
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
  );
};

export default TextInput;

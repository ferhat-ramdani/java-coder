# JavaCoder

JavaCoder is an advanced, self-contained AI assistant designed specifically to act as a powerful and independent developer companion. By deeply integrating local large language models directly into the Java development lifecycle, JavaCoder provides an unparalleled experience that goes far beyond generic chat interfaces.

## Core Capabilities

*   **Intelligent Auto-Correction Loop:** JavaCoder doesn't just generate code; it verifies it. The assistant features an internal auto-correction loop that actively compiles the generated Java classes *before* presenting them to you, ensuring the code you receive is structurally sound and immediately usable.
*   **Agent-Like Streaming Process:** Experience real-time collaboration. JavaCoder utilizes an agent-like streaming architecture that processes and delivers code incrementally, making the interaction feel seamless and highly responsive.
*   **Zero-Friction, OS-Agnostic Startup:** Say goodbye to complex environment setups. JavaCoder is equipped with a powerful, built-in startup script that automatically detects your operating system, seamlessly installs Ollama, and dynamically pulls the necessary lightweight models entirely on its own. 

## Dynamic Model Management

JavaCoder is designed to be lightweight by default, initializing only with what is strictly necessary (e.g., `llama3.2 3B`). However, it provides full flexibility through its dynamic model management system:
*   **On-the-Fly Installation:** Easily browse and install new local models directly through the application's interface. 
*   **Automatic Provisioning:** Once a new model is requested, the system automatically pulls, installs, and registers the model in the backend, making it instantly available for your next chat session without any manual terminal intervention.

## Architecture Highlights

JavaCoder is built for performance and simplicity, packaged as a monolithic executable:
*   **Backend:** Powered by Helidon SE, delivering ultra-fast REST APIs and robust orchestration for LLM communication and the compilation auto-correction loop.
*   **Frontend:** A reactive Single Page Application (SPA) built with SolidJS, tightly integrated into the backend's build process for a single-artifact deployment.
*   **Data Storage:** An embedded, auto-configured H2 database persistently stores all your context, chat histories, and dynamically added models.

## Execution

Simply compile and run the self-contained executable JAR. JavaCoder's automated startup script will handle the rest:

```bash
mvn clean package
java -jar target/java_coder.jar
```


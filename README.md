# JavaCoder

JavaCoder is an advanced, self-contained AI assistant designed specifically to act as a powerful and independent developer companion. By deeply integrating local large language models directly into the Java development lifecycle, JavaCoder provides an unparalleled experience that goes far beyond generic chat interfaces.

## Core Capabilities

*   **Intelligent Auto-Correction Loop:** JavaCoder doesn't just generate code; it verifies it. Every generated class is compiled, checked for a real `public static void main(String[] args)` entry point, and actually *executed* in an isolated sandbox to catch runtime exceptions - all before it's shown to you. Compiler errors and stack traces are fed straight back to the model so it can fix its own mistakes, up to 3 attempts.
*   **Sandboxed, Interactive Execution:** Generated code never runs on your machine directly. It's executed inside a throwaway, network-less Docker container with hard memory/CPU/process limits. Programs that read user input (`Scanner`, `System.in`) work as expected: the chat's inline terminal streams output live and lets you type input back, just like a real terminal.
*   **Agent-Like Streaming Process:** Experience real-time collaboration. JavaCoder streams every step of the generation pipeline (generating, compiling, sandbox-testing, retrying) so you can watch the assistant work, with the full history available on demand.
*   **Zero-Friction, OS-Agnostic Startup:** Say goodbye to complex environment setups. JavaCoder is equipped with a powerful, built-in startup script that automatically detects your operating system, seamlessly installs Ollama, and dynamically pulls the necessary lightweight models entirely on its own.

### Prerequisite: Docker

Safe code execution (the auto-correction loop's runtime check, and the "Run" button/interactive terminal) requires [Docker](https://www.docker.com/products/docker-desktop/) to be installed and running. JavaCoder detects Docker at startup and pulls its small sandbox runtime image (`eclipse-temurin:21-jre-alpine`) automatically in the background. If Docker isn't available, chat and code generation still work normally - only the execution/sandbox-verification step is skipped, with a clear message explaining why.

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


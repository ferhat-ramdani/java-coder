# JavaCoder

Tired of Java homework? Use JavaCoder to generate your own code!

## How to use

1. Clone this repository
```bash
git clone https://gitlab.com/SportDay/chatgpt-for-dev
```


2. Run the following command in the terminal to compile the project

```bash
mvn clean package
```

> **Note :** build file is located in `target` directory


3. Run the following command to execute the program
```bash
java -jar java_coder.jar
```

## UI Pages

- **Main Page:** http://localhost:8080/
- **Chat List Page:** http://localhost:8080/chats
- **Chat Page:** http://localhost:8080/chats/{id}
- **LLM Page:** http://localhost:8080/llms
- **Swagger:** http://localhost:8080/openapi

## Other
- Database are stored in `~/.javacoder/database` directory

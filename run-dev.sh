#!/bin/bash

echo "Building the project..."
if mvn clean package -DskipTests; then
    echo "Starting the application..."
    java -jar target/java_coder.jar
else
    echo "Build failed!"
    exit 1
fi

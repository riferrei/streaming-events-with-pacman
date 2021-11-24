#!/bin/bash

rm -rf deploy
mkdir -p deploy

mvn clean package
mv target/alexa-handler-function-1.0.jar deploy/alexa-handler-function-1.0.jar

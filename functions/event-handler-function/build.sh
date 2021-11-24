#!/bin/bash

rm -rf deploy
mkdir -p deploy

mvn clean package
mv target/event-handler-function-1.0.jar deploy/event-handler-function-1.0.jar

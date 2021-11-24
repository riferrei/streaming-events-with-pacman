#!/bin/bash

rm -rf deploy
mkdir -p deploy

mvn clean package
mv target/scoreboard-function-1.0.jar deploy/scoreboard-function-1.0.jar

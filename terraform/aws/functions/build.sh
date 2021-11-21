#!/bin/bash

rm -rf deploy
mkdir -p deploy

mvn clean package
mv target/elastic-o11y-for-aws-1.0.jar deploy/elastic-o11y-for-aws-1.0.jar

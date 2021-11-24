#!/bin/bash

./functions-logging-prepare.sh
cd functionbeat

./functionbeat -c functionbeat.yml -e setup
./functionbeat -c functionbeat.yml -e -v -d "*" deploy event-handler-logs
./functionbeat -c functionbeat.yml -e -v -d "*" deploy scoreboard-logs
./functionbeat -c functionbeat.yml -e -v -d "*" deploy alexa-handler-logs

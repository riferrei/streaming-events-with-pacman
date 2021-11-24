#!/bin/bash

./functions-logging-prepare.sh
cd functionbeat

./functionbeat -c functionbeat.yml -e -v -d "*" remove event-handler-logs
./functionbeat -c functionbeat.yml -e -v -d "*" remove scoreboard-logs
./functionbeat -c functionbeat.yml -e -v -d "*" remove alexa-logs

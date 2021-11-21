#!/bin/bash

cd functionbeat
./functionbeat -c functionbeat.yml -e -v -d "*" remove alexa-logs 
./functionbeat -c functionbeat.yml -e -v -d "*" remove scoreboard-logs

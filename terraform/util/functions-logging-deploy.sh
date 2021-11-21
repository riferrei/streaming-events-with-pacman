#!/bin/bash

cd functionbeat
./functionbeat -c functionbeat.yml -e setup
./functionbeat -c functionbeat.yml -e -v -d "*" deploy alexa-logs 
./functionbeat -c functionbeat.yml -e -v -d "*" deploy scoreboard-logs

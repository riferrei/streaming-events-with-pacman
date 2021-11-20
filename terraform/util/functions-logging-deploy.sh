#!/bin/bash

cd functionbeat
./functionbeat -c functionbeat.yml -e setup
./functionbeat -c functionbeat.yml -e -v -d "*" deploy logging

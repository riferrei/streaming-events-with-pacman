#!/bin/bash

cd functionbeat
./functionbeat -c functionbeat.yml -e -v -d "*" remove logging

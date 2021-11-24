#!/bin/bash

if [ ! -d "functionbeat" ]
then
    FILENAME="functionbeat.tar.gz"
    if [ ! -f "${FILENAME}" ]
    then
        PLATFORM=`uname -m`
        OPERATING_SYSTEM="linux"
        if [[ $OSTYPE == 'darwin'* ]]; then
            OPERATING_SYSTEM="darwin"
        fi
        URL="https://artifacts.elastic.co/downloads/beats/functionbeat/functionbeat-7.13.0-${OPERATING_SYSTEM}-${PLATFORM}.tar.gz"
        curl -L "${URL}" > "${FILENAME}"
    fi
    mkdir functionbeat && tar xf functionbeat.tar.gz -C functionbeat --strip-components 1 && rm "${FILENAME}"
fi

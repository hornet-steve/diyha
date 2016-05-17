#!/bin/bash

nohup java -Dconfig.file=application.conf -Dconfig.trace=loads -jar diyha_station.jar >/dev/null 2>&1 & PID=$!
echo $PID > diyha_station.pid
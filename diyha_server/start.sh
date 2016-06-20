#!/bin/bash

nohup ./bin/diyha_server >/dev/null 2>&1 & PID=$!
echo $PID > diyha_server.pid
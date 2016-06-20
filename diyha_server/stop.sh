#!/bin/bash

PID=$(cat diyha_server.pid)
kill $PID
rm -rf diyha_server.pid
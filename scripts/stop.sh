#!/bin/bash

PID=$(cat diyha_station.pid)
kill $PID
rm -rf diyha_station.pid
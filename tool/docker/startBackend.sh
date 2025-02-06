#!/bin/bash
nohup java -jar /data/deploy/train/train.jar --spring.profiles.active=dev -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dspring.output.ansi.enabled=always --server.port=8090 &

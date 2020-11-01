#!/bin/sh

# call curl only once to ensure we get a matching PHPSESSID->CSRF pair.
curl --silent -i 192.168.56.9/login --output index.html 2>&1

# extract the PHPSESSID and CSRF token from the response.
export PHPSESSID=$(cat index.html | egrep -o '(PHPSESSID)=[a-z0-9]+?' | cut -d '=' -f2)
export CSRF=$(cat index.html | grep -A1 'csrf' | cut -d '=' -f2 | tail -n 1 | tr -d '"')

# verify that environment variables are updated.
printenv | egrep 'PHPSESSID|CSRF'

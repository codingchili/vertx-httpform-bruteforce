# vertx-httpform-bruteforce
Vertx client to brute force http-post form, twice as fast as hydra.

### Building

Modify Breaker.kt to update form parameters.

```
./gradlew build
```

### Usage

Grab a session id and csrf token if required, can be done using tokens.sh

```
. ./tokens.sh

#java -jar form-breaker-1.0-SNAPSHOT.jar <username> <sessionId> <csrf-token> <wordlist.txt>
java -jar form-breaker-1.0-SNAPSHOT.jar test $PHPSESSID $CSRF rockyou.78.txt
```

Wordlist is a file with passwords to try, separated by newlines.
For example, to filter the rockyou.txt to a specific passlength

```
$ awk '{ if (length($0) == 8 || length($0) == 7) print }' rockyou.txt >rockyou.7-8.txt
```

Run snapshot.sh with a known valid login to periodically sanity check the server.

```
./snapshot.sh
```

If the server stops responding, recompile and set the wordlist offset.

### Performance

Twice as fast as hydra, gets about 15-22k requests/minute on a local virtualbox target.

Loads the whole wordlist in memory.. so prefer to filter into smaller lists, or submit a PR. :kissing_cat:

#!/bin/bash
mvn -Pexec clean install assembly:single

rm ./bin/postman-tools 2> /dev/null # ignore if not there
cat ./bin/stub.sh ./target/postman-runner-3.0.0-SNAPSHOT-jar-with-dependencies.jar > ./bin/postman-tools
chmod +x ./bin/postman-tools
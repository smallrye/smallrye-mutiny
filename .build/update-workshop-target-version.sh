#!/usr/bin/env bash
./mvnw -Pupdate-workshop-examples -f workshop-examples compile -DworkshopVersion=$1
find workshop-examples -name '*.java' | xargs chmod +x
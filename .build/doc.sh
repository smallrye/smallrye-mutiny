#!/usr/bin/env bash
echo "Building the doc"
cd documentation
mvn clean package


echo "Cloning repo"
cd target
git clone -b gh-pages git@github.com:smallrye/smallrye-mutiny.git site
echo "Copy content"
cp -R generated-docs/* site

echo "Pushing"
cd site
git add -A
git commit -m "update site"
git push origin gh-pages

echo "Done"

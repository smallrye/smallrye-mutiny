#!/usr/bin/env bash
echo "🚧 Building..."
cd documentation || exit 1
mvn clean test && cd src/main/jekyll && gem install bundler && bundle install && JEKYLL_ENV=production bundle exec jekyll build  && cd - || exit 1
echo "🍺 Site generated in 'target/_site'"

echo "🚧 Cloning web site in target/site"
cd target || exit
git clone -b gh-pages "https://${GITHUB_ACTOR:-cescoffier}:${GITHUB_TOKEN}@github.com/smallrye/smallrye-mutiny.git" site
echo "🚧 Copy content"
# shellcheck disable=SC2216
rm -rf site/*
cp -R _site/* site
echo "🚧 Pushing documentation"
cd site || exit
git add -A
git commit -m "update site"
git push origin gh-pages
echo "🍺 Site updated!"
cd ../../..


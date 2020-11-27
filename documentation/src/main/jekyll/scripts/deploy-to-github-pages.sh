#!/bin/bash

BRANCH=gh-pages
DIR=target

rm -Rf $DIR
mkdir -p $DIR
cd $DIR
git clone https://${JEKYLL_PAT}@github.com/cescoffier/mutiny-doc-sandbox.git site
cd site
git fetch origin
git checkout $BRANCH
ls

echo "Copying site"
cp -R ../../_site/* .
git add --all assets/*
git add -A
git commit -am "Update web site"
git status
git push origin $BRANCH

echo "Web site updated..."

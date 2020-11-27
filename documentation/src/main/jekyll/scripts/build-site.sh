#!/bin/bash
echo "Building site"
rm -Rf _site
bundle exec jekyll build
echo "Site generated in _site"

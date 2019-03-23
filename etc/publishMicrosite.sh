#!/bin/sh
set -e

git config --global user.email "amilkov3@gmail.com"
git config --global user.name "amilkov3"
git config --global push.default simple

sbt docs/publishMicrosite

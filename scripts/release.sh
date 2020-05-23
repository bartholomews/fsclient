#!/usr/bin/env bash

TAG=$1
git tag -a ${TAG} -m ${TAG} \
&& git push origin ${TAG}
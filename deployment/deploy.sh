#! /bin/bash

set -e

ssh -t $1 "export GIT_SSH=~/schema-summaries/deployment/git+ssh.sh && cd ~/schema-summaries && git checkout -f master && ./install.sh $2"


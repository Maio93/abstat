#! /bin/bash

set -e

ssh $1 "export GIT_SSH=~/schema-summaries/scripts/git+ssh.sh && cd ~/schema-summaries && git pull"


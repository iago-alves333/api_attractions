FROM ubuntu:latest
LABEL authors="iago"

ENTRYPOINT ["top", "-b"]
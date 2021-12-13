FROM docker.elastic.co/beats/heartbeat:7.16.1-arm64
COPY heartbeat.yml /usr/share/heartbeat/heartbeat.yml
USER root
RUN chown root:heartbeat /usr/share/heartbeat/heartbeat.yml
USER heartbeat

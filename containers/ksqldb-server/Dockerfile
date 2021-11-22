FROM confluentinc/ksqldb-server:0.22.0
COPY queries.sql /etc/ksqldb/queries.sql
USER root
RUN chown root:appuser /etc/ksqldb/queries.sql
USER appuser

# Run James
#
# VERSION	1.0

FROM adoptopenjdk:11-jdk-hotspot AS jre-builder

WORKDIR /fs

# Get data we need to run James: build results and configuration
ADD destination/james-server-cassandra-rabbitmq-guice.jar /fs/james-server.jar
ADD destination/james-server-cassandra-rabbitmq-guice.lib /fs/james-server-cassandra-rabbitmq-guice.lib
ADD destination/james-server-cli.jar /fs/james-cli.jar
ADD destination/james-server-cli.lib /fs/james-server-cli.lib
ADD destination/conf /fs/conf
ADD destination/glowroot/plugins /fs/glowroot/plugins
ADD destination/glowroot/glowroot.jar /fs/glowroot/glowroot.jar
ADD destination/glowroot/lib/glowroot-embedded-collector.jar /fs/glowroot/lib/glowroot-embedded-collector.jar
ADD destination/glowroot/lib/glowroot-logging-logstash.jar /fs/glowroot/lib/glowroot-logging-logstash.jar
ADD destination/glowroot/admin.json /fs/glowroot/admin.json
ADD destination/run_james.sh /fs/run_james.sh

RUN jlink \
        --verbose \
        --add-modules \
        $(java --list-modules /fs/james-server-cassandra-rabbitmq-guice.jar >&1 | sed 's/@.*$/,/g' | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/ /g' | sed 's/ //g') \
        --no-header-files \
        --no-man-pages \
        --output /opt/jre
        # --compress 2 \
        # --strip-debug \

# Final image
FROM alpine:3.10

ENV PATH = "$PATH:/opt/jre/bin"
COPY --from=jre-builder /opt/jre /opt/jre
COPY --from=jre-builder /fs /root

# Ports that are used
#
# 25   SMTP without authentication
# 110  POP3
# 143  IMAP with startTLS enabled
# 465  SMTP with authentication and socketTLS enabled
# 587  SMTP with authentication and startTLS enabled
# 993  IMAP with socketTLS enabled
# 8000 Web Admin interface (unsecured: expose at your own risks)

EXPOSE 25 110 143 465 587 993 4000 8000

VOLUME /logs
VOLUME /root/conf
VOLUME /root/glowroot/plugins
VOLUME /root/glowroot/data

ENV PATH "$PATH:/root/glowroot/lib" \
    JVM_OPTIONS="" \
    GLOWROOT_ACTIVATED="false"

ENTRYPOINT ./run_james.sh

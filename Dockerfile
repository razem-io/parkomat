FROM openjdk:8

ADD server/target/universal/parkomat-*.tgz /

RUN mv /parkomat* /parkomat && chmod u+x /parkomat/bin/server

ENTRYPOINT ["/parkomat/bin/server"]
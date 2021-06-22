FROM openjdk:11
WORKDIR /
COPY build/libs/GitRules-1.0-SNAPSHOT.jar App.jar
COPY input/ input/
CMD java -jar App.jar -Xmx32G

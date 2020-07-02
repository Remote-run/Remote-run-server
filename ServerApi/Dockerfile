FROM tomcat:jdk14-openjdk-oracle

ENV SQLURL="jdbc:postgresql://localhost:54320/docker"
ENV POSGRESS_USER="postgres"
ENV POSGRESS_PASSWORD="mysecretpassword"

# this is requiered to alow the application to run insted of the welcome one
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/Server.war /usr/local/tomcat/webapps/ROOT.war
#COPY ./target/Server.war /usr/local/tomcat/webapps/Server.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
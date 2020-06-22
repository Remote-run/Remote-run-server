FROM tomcat:latest

ARG SharedDir

# this is requiered to alow the application to run insted of the welcome one
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/Server.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
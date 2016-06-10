FROM tomcat:8-jre8
MAINTAINER martin scharm

# remove the default tomcat application
RUN rm -rf /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/ROOT.war

# add the CombineArchive Web interface as the new default web app
COPY CombineArchiveWeb-DockerContext.xml /usr/local/tomcat/conf/Catalina/localhost/ROOT.xml
COPY CombineArchiveWeb-${project.version}.war /usr/local/tomcat/webapps/ROOT.war

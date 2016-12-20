# WebCAT

The [CombineArchive](http://co.mbine.org/standards/omex) is a zip like format to bundle latest research files and annotate them with meta data. The [CombineArchive WebInterface (WebCAT)](https://github.com/SemsProject/CombineArchiveWeb) is a web based user interface to create, modify, and share CombineArchives. No need for registration. No database. Just start using the web interface!

## Build and Run

You will find detailed information on how to build and deploy the web application on the [project page](https://sems.uni-rostock.de/trac/combinearchive-web).



## Docker

There is a Docker image available at [binfalse/webcat](https://hub.docker.com/r/binfalse/webcat/). It is configured to automatically build the latest version of the application based on the [Dockerfile](Dockerfile) included in this repository. In addition, we compile every version of the application in a tagged Docker image [through maven](https://binfalse.de/2016/05/31/mvn-automagically-create-a-docker-image/).

Run a Docker container as:

    docker run -it --rm -p 1234:8080 binfalse/webcat:latest

This will start the Tomcat web server and bind the host's port `1234` to the containers port `8080`, which Tomcat listens at. It will permanently dump Tomcat's catalina output log.

As soon as the container is up and running you should be able to access the CombineArchive web interface at http://localhost:1234

### Save Workspaces and COMBINE archives
The above command will destroy all archives and workspaces as soon as you stop/reboot the container.
It's however also possible to mount persistent storage into the container. Let's say you want to save everything at the host's `/storage/for/webcat`, then you can just mount that into the container using:

    docker run  --rm -it -p 1234:8080 -v /storage/for/webcat:/srv/CombineArchiveWeb binfalse/webcat:latest

After creating a COMBINE archive in WebCAT you'll find it on the host machine in `/storage/for/webcat/WORKSPACE/ARCHIVE`.

### Modify the Settings of WebCAT
WebCAT's settings are configured using a tomcat-style `context.xml`. The [context that is deployed by default is available from the git repository,](https://github.com/binfalse/CombineArchiveWeb/blob/master/src/main/docker/CombineArchiveWeb-DockerContext.xml) you may use that as a template.

To change that configuration you can either mount a directory containing a file called `ROOT.xml` with an alternative configuration to the container's `/usr/local/tomcat/conf/Catalina/localhost/` directory :

    docker run  --rm -it -p 1234:8080 -v /storage/dir/with/other/context:/usr/local/tomcat/conf/Catalina/localhost/ binfalse/webcat:latest

Or you create a new image based on this one, which overwrites the configuration with your `context.xml`.




## Demo
A demo is currently hosted on our server. *No need for registration!*
[Just start to explore](http://webcat.sems.uni-rostock.de)



## Development
The development is currently coordinated at our [trac project management](https://sems.uni-rostock.de/trac/combinearchive-web).



## Additional Resources
Some helpful resources can be found in the resources folder. It contains a default context configuration for tomcat, a default
VHost configuration for the Apache WebServer and a folder called "nagios" containing a simple python3 script for nagios, to
check if the webCAT instance is up and running. In case a stats secret is provided (and configured in the python script) it
also checks for the maximum space quota.


## Request for comments
We are always keen on getting feedback from our users. If you have any comments, requests for features, or experience any problems [do not hesitate to leave a comment.](https://github.com/SemsProject/CombineArchiveWeb/issues/new)


## Licence
    CombineArchiveWeb - a WebInterface to read/create/write/manipulate/... COMBINE archives
    Copyright (C) 2014-2016:
     - Martin Peters <martin@freakybytes.net>
     - Martin Scharm <martin@binfalse.de>
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
      
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.


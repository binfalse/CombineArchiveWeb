# webCAT
This webapplication is a part of the [CombineArchiveToolkit](https://sems.uni-rostock.de/projects/combinearchive/), a selection of tools to
create, modify, and share CombineArchives.
Find more information on how to build and deploy it on the [project page](https://sems.uni-rostock.de/trac/combinearchive-web).

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

## Licence
CombineArchiveWeb - a WebInterface to read/create/write/manipulate/... COMBINE archives
Copyright (C) 2014-2015:
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


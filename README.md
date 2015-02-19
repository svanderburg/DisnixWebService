DisnixWebService
================
DisnixWebService is an extension for the Disnix toolset allowing it to connect
to the `disnix-service` instances running on target machines through a Java-based
web service/SOAP interface.

The primary purpose of this package is to demonstate how such an extension can be
developed, so that integration of Disnix can be made more convenient. This
package has not been developed for production usage.

Prerequisites
=============
In order to build this package from source code the following packages are
required:

* [dbus-java](http://www.freedesktop.org/wiki/Software/DBusBindings) to connect to the `disnix-service` instance
* [Apache Axis2](http://axis.apache.org/axis2/java/core) for implementing the SOAP layer
* [Apache Ant](http://ant.apache.org) is required to build the project

To run the package the following packages must be installed:

* A working [Disnix](https://github.com/svanderburg/disnix) installation. Consult its documentation for more information on its installation details.
* A Java Servlet container. Currently only [Apache Tomcat](http://tomcat.apache.org) has been tested.

Installation
============
Besides a working Disnix installation, you need a running Java servlet container,
compile this package from source code with Apache Ant and install the
corresponding WAR file in the servlet container. The manual of this package
provides for more details on how to this with Apache Tomcat.

Usage
=====
To use the `DisnixWebService` while deploying a system, you must add a property
to the `infrastructure.nix` model that specifies how to connect to it. For
example:

    {
      target1 = {
        hostname = "target1";
        tomcatPort = 8080;
        targetEPR = http://target1:8080/DisnixService/services/DisnixService;
      };
    }

The above model defines a `targetEPR` attribute containing the URL to the
`DisnixWebService`.

By setting the following environment variable, Disnix is instructed to use
`targetEPR` to connect to the remote Disnix service:

    $ export DISNIX_TARGET_PROPERTY=targetEPR

By setting the following environment variable, Disnix uses the
`disnix-soap-client` interface to establish the remote connection:

    $ export DISNIX_CLIENT_INTERFACE=disnix-soap-client

Manual
======
Disnix has a nice Docbook manual that can be compiled yourself. However, it is
also available [online](http://hydra.nixos.org/job/disnix/DisnixWebService-trunk/tarball/latest/download-by-type/doc/manual).

License
=======
This package is released under the [MIT license](http://opensource.org/licenses/MIT).

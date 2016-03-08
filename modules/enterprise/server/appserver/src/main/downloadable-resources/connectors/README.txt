This directory contains connectors that instrument various products to expose
monitoring data. This monitoring data can then be accessed by the RHQ plugins
corresponding to those products.

HTTP Response Time Filter
--------------------------
This is a servlet filter that can measure response times for HTTP requests
made to servlets and write the response times to a logfile. The filter is
compatible with any servlet 2.4 or later container running on Java 1.4 or 
later. However, since upport for parsing the response time logfiles is
only provided by the jboss-as, jboss-as-5, jboss-as-7, wfly-10, and tomcat RHQ
plugins, it typically is only of value to deploy the filter to JBoss AS, Wildfly
or Tomcat.

Note, a commons-logging jar is supplied for use with Tomcat 6. It is not 
required for earlier versions of Tomcat, since they already include that jar.

For details on how to deploy the filter to various versions of JBoss AS and
Tomcat, see:

https://docs.jboss.org/author/display/RHQ/Managed+Product+Configuration


This directory contains connectors that instrument various products to expose
monitoring data. This monitoring data can then be accessed by the RHQ plugins
corresponding to those products.

HTTP Response Time Filter
--------------------------
This is a servlet filter that can measure response times for HTTP requests
made to servlets. The filter currently supports:

- Embedded JBossAS Tomcat
- Standalone JBoss EWS Tomcat
- Standalone Apache EWS Tomcat 

Note, a commons-logging jar is supplied for use with Standalone Tomcat6. It
is not required for ealier, supported versions (e.g 5.5).

The Tomcat Server must be instrumented with the Filter. For information on
configuring the filter, see the Response Times section at the following URL:

http://www.redhat.com/docs/en-US/JBoss_ON/html/Feature_Guide/index.html
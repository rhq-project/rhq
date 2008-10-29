This directory contains connectors that instrument various products to expose
monitoring data. This monitoring data can then be accessed by the RHQ plugins
corresponding to those products.

HTTP Response Time Filter
--------------------------
This is a servlet filter that can measure response times for HTTP requests
made to servlets. The filter is currently only supported for a Tomcat instance
embedded within a JBossAS instance. For information on configuring the filter,
see the following URL:

https://docs.jbosson.redhat.com/confluence/display/JON2/Response+Time+Filter
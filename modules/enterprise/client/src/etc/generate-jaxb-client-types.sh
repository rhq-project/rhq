#!/bin/sh

# This script should consume all the wsdls and compile the JAXB types all
# into one directory.

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/RoleManagerBean?wsdl -p org.rhq.enterprise.server.ws

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/ContentManagerBean?wsdl -p org.rhq.enterprise.server.ws

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/SubjectManagerBean?wsdl -p org.rhq.enterprise.server.ws

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/OperationManagerBean?wsdl -p org.rhq.enterprise.server.ws

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/ChannelManagerBean?wsdl -p org.rhq.enterprise.server.ws

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/ConfigurationManagerBean?wsdl -p org.rhq.enterprise.server.ws

../../../../dev-container/jbossas/bin/wsconsume.sh -k http://localhost:7080/rhq-rhq-enterprise-server-ejb3/ResourceManagerBean?wsdl -p org.rhq.enterprise.server.ws
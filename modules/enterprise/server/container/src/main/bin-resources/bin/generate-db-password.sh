#!/bin/sh

# the sleep is important to prevent a race condition when called from the installer
sleep 2

# We stand in $RHQ/bin when called from the installer
cd ../jbossas/

java -cp lib/jboss-common.jar:lib/jboss-jmx.jar:server/default/lib/jbosssx.jar:server/default/lib/jboss-jca.jar org.jboss.resource.security.SecureIdentityLoginModule $*

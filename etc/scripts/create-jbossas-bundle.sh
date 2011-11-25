#!/bin/sh

if [ $# -ne 1 ]; then
   echo "Usage: $0 jboss-as-or-eap-dist.zip" >&2
   exit 1
fi

JBOSS_ZIPFILE=$1
JBOSS_ZIPFILENAME=`basename $JBOSS_ZIPFILE`

ORIG_PWD=`pwd`

# Unzip the JBoss zipfile under a temp dir.
TMPDIR=`mktemp --directory`
cp $JBOSS_ZIPFILE $TMPDIR
cd $TMPDIR
unzip $JBOSS_ZIPFILENAME >/dev/null
rm $JBOSS_ZIPFILENAME

# Remove the JBoss files we don't want included in our bundle.
cd jboss*
JBOSS_BASEDIR=`pwd`
if [ -d jboss-as ]; then
   cd jboss-as
   EAP=1   
fi
JBOSS_AS_DIR=`pwd`

rm -f bin/*.bat
rm -rf docs

if [ "$EAP" = 1 ]; then
   JBOSS_CONFIG_DIR="server/production"
   cd server   
   rm -rf all default minimal standard web
else    
   JBOSS_CONFIG_DIR="server/default"
   cd server
   rm -rf all minimal
fi

# Recreate the JBoss zipfile now that we've removed everything we don't want in
# the bundle. But get rid of the top-level directory, since that is managed by
# the RHQ bundle deployer.
cd $JBOSS_AS_DIR
zip -r9 $TMPDIR/$JBOSS_ZIPFILENAME * >/dev/null
rm -rf $JBOSS_BASEDIR

# Create the bundle recipe file.
cd $TMPDIR
cat >deploy.xml <<EOF
<?xml version="1.0"?>

<project name="test-bundle" default="main" xmlns:rhq="antlib:org.rhq.bundle">

    <rhq:bundle name="jbossas" version="1.0" description="JBoss AS">
        <rhq:deployment-unit name="appserver" preinstallTarget="preinstall" postinstallTarget="postinstall">
            <rhq:archive name="${JBOSS_ZIPFILENAME}" exploded="true">
                <rhq:replace>
                    <rhq:fileset includes="**/*.properties"/>
                </rhq:replace>
            </rhq:archive>
            <rhq:ignore>
                <rhq:fileset includes="${JBOSS_CONFIG_DIR}/data/**"/>
                <rhq:fileset includes="${JBOSS_CONFIG_DIR}/log/**"/>
                <rhq:fileset includes="${JBOSS_CONFIG_DIR}/tmp/**"/>
                <rhq:fileset includes="${JBOSS_CONFIG_DIR}/work/**"/>
            </rhq:ignore>
        </rhq:deployment-unit>
    </rhq:bundle>

    <target name="main" />

    <target name="preinstall">
        <echo>Deploying JBoss AS Bundle v1.0 to \${rhq.deploy.dir}...</echo>
        <property name="preinstallTargetExecuted" value="true"/>
        <rhq:audit status="SUCCESS" action="Preinstall Notice" info="Preinstalling to \${rhq.deploy.dir}" message="Another optional message">
           Some additional, optional details regarding the deployment of \${rhq.deploy.dir}
        </rhq:audit>
    </target>

    <target name="postinstall">
        <echo>Done deploying JBoss AS Bundle v1.0 to \${rhq.deploy.dir}.</echo>
        <property name="postinstallTargetExecuted" value="true"/>
    </target>
</project>
EOF

# Create a bundle zipfile containing the contents of the temp dir.
JBOSS_ZIPFILEBASENAME=`echo $JBOSS_ZIPFILENAME | sed 's/\.zip//'`
BUNDLE_ZIPFILENAME="$JBOSS_ZIPFILEBASENAME-bundle.zip"
zip -r9 $ORIG_PWD/$BUNDLE_ZIPFILENAME * >/dev/null
cd $ORIG_PWD
echo "Bundle created: $BUNDLE_ZIPFILENAME"


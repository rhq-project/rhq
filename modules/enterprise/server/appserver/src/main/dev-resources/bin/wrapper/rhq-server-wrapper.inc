#
# Additional dev-only options for the RHQ Server Java Service Wrapper. Numbering should start at N+1 where N is the
# last wrapper.java.additional.N entry in rhq-server-wrapper.conf.
#

# reduce memory needs for dev env.
wrapper.java.additional.25=-Xms512M
wrapper.java.additional.26=-Xmx512M
wrapper.java.additional.27=-XX:PermSize=200M
wrapper.java.additional.28=-XX:MaxPermSize=200M

# enable remote debugging
wrapper.java.additional.29=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n

# enable jprofiler
#set.PATH=%PATH%;<jprofiler-install-dir>\bin\windows
#wrapper.java.additional.30=-agentlib:jprofilerti=port=8849
#wrapper.java.additional.31=-Xbootclasspath/a:<jprofiler-install-dir>\bin\agent.jar

# disable JVM startup timeout
wrapper.startup.timeout=0
# disable JVM ping timeout
wrapper.ping.timeout=0

#
# Additional dev-only options for the RHQ Server Java Service Wrapper. Numbering should start at N+1 where N is the
# last wrapper.java.additional.N entry in rhq-server-wrapper.conf.
#

# enable remote debugging
wrapper.java.additional.9=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n
# override and lower the initial memory profile
wrapper.java.additional.10=-Xms256M
wrapper.java.additional.11=-Xmx512M
wrapper.java.additional.12=-XX:PermSize=128M
wrapper.java.additional.13=-XX:MaxPermSize=128M

# enable jprofiler
#set.PATH=%PATH%;<jprofiler-install-dir>\bin\windows
#wrapper.java.additional.14=-agentlib:jprofilerti=port=8849
#wrapper.java.additional.15=-Xbootclasspath/a:<jprofiler-install-dir>\bin\agent.jar

# disable JVM startup timeout
wrapper.startup.timeout=0
# disable JVM ping timeout
wrapper.ping.timeout=0

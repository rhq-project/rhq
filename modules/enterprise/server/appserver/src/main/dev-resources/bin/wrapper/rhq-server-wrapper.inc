#
# Additional dev-only options for the RHQ Server Java Service Wrapper. Numbering should at N+1 where N is the
# last wrapper.java.additional.N entry in rhq-agent-wrapper.conf.
#

# enable remote debugging
wrapper.java.additional.9=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n

# enable jprofiler
#set.PATH=%PATH%;<jprofiler-install-dir>\bin\windows
#wrapper.java.additional.10=-agentlib:jprofilerti=port=8849
#wrapper.java.additional.11=-Xbootclasspath/a:<jprofiler-install-dir>\bin\agent.jar

# disable JVM startup timeout
wrapper.startup.timeout=0
# disable JVM ping timeout
wrapper.ping.timeout=0

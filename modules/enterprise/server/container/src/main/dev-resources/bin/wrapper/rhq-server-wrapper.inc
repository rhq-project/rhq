#
# Additional dev-only options for the RHQ Server Java Service Wrapper
#

# enable remote debugging
wrapper.java.additional.12=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n

# enable jprofiler
#set.PATH=%PATH%;<jprofiler-install-dir>\bin\windows
#wrapper.java.additional.13=-agentlib:jprofilerti=port=8849
#wrapper.java.additional.14=-Xbootclasspath/a:<jprofiler-install-dir>\bin\agent.jar

# disable JVM startup timeout
wrapper.startup.timeout=0
# disable JVM ping timeout
wrapper.ping.timeout=0

#
# Additional dev-only options for the RHQ Server Java Service Wrapper
#

# enable remote debugging
wrapper.java.additional.12=-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=n

# disable JVM startup timeout
wrapper.startup.timeout=0
# disable JVM ping timeout
wrapper.ping.timeout=0

#
# Additional dev-only options for the RHQ Storage Java Service Wrapper. Numbering should start at N+1 where N is the
# last wrapper.java.additional.N entry in rhq-storage-wrapper.conf.
#

# override and lower the initial memory profile
wrapper.java.additional.20=-Xms128M
wrapper.java.additional.21=-Xmx256M

# enable remote debugging
#wrapper.java.additional.22=-agentlib:jdwp=transport=dt_socket,address=8788,server=y,suspend=n

# disable JVM startup timeout
wrapper.startup.timeout=0
# disable JVM ping timeout
wrapper.ping.timeout=0

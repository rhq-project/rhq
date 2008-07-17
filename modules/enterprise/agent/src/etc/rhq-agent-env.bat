rem ===========================================================================
rem RHQ Agent Windows Startup Script Configuration File
rem ===========================================================================
rem
rem This file is optional; it may be removed if not needed.
rem

rem    RHQ_AGENT_DEBUG - If this is defined (with any value), the script
rem                      will emit debug messages. It will also enable debug
rem                      messages to be emitted from the agent itself.
rem
rem set RHQ_AGENT_DEBUG=1

rem    RHQ_AGENT_HOME - Defines where the agent's home install directory is.
rem                     If not defined, it will be assumed to be the parent
rem                     directory of the directory where this script lives.
rem
rem set RHQ_AGENT_HOME=C:\opt\rhq-agent

rem    RHQ_AGENT_JAVA_HOME - The location of the JRE that the agent will
rem                          use. This will be ignored if
rem                          RHQ_AGENT_JAVA_EXE_FILE_PATH is set.
rem                          If this and RHQ_AGENT_JAVA_EXE_FILE_PATH are
rem                          not set, the agent's embedded JRE will be used.
rem
rem set RHQ_AGENT_JAVA_HOME=C:\opt\java

rem    RHQ_AGENT_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                                   executable to use. If this is set,
rem                                   RHQ_AGENT_JAVA_HOME is ignored.
rem                                   If this is not set, then
rem                                   %RHQ_AGENT_JAVA_HOME%\bin\java.exe
rem                                   is used. If this and
rem                                   RHQ_AGENT_JAVA_HOME are not set, the
rem                                   agent's embedded JRE will be used.
rem
rem set RHQ_AGENT_JAVA_EXE_FILE_PATH=C:\WINDOWS\system32\java.exe

rem    RHQ_AGENT_JAVA_OPTS - Java VM command line options to be
rem                          passed into the agent's VM. If this is not defined
rem                          this script will pass in a default set of options.
rem                          If this is set, it completely overrides the
rem                          agent's defaults. If you only want to add options
rem                          to the agent's defaults, then you will want to
rem                          use RHQ_AGENT_ADDITIONAL_JAVA_OPTS instead.
rem
rem set RHQ_AGENT_JAVA_OPTS=-Xms160M -Xmx160M -Djava.net.preferIPv4Stack=true

rem    RHQ_AGENT_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
rem                                     to be passed into the agent's VM. This
rem                                     is added to RHQ_AGENT_JAVA_OPTS; it
rem                                     is mainly used to augment the agent's
rem                                     default set of options. This can be
rem                                     left unset if it is not needed.
rem
rem set RHQ_AGENT_ADDITIONAL_JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=9797,server=y,suspend=n

rem    RHQ_AGENT_CMDLINE_OPTS - If this is defined, these are the command line
rem                             arguments that will be passed to the RHQ Agent.
rem                             If this is not defined, the command line
rem                             arguments given to this script are passed
rem                             through to the RHQ Agent.
rem
rem set RHQ_AGENT_CMDLINE_OPTS=--nonative

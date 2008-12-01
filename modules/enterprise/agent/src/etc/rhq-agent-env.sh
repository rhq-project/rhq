#===========================================================================
# RHQ Agent UNIX Startup Script Configuration File
#===========================================================================
#

#    RHQ_AGENT_DEBUG - If this is defined (with any value), the script
#                      will emit debug messages. It will also enable debug
#                      messages to be emitted from the agent itself.
#
#RHQ_AGENT_DEBUG=1

#    RHQ_AGENT_HOME - Defines where the agent's home install directory is.
#                     If not defined, it will be assumed to be the parent
#                     directory of the directory where this script lives.
#
#RHQ_AGENT_HOME=/opt/rhq-agent 

#    RHQ_AGENT_JAVA_HOME - The location of the JRE that the agent will
#                          use. This will be ignored if
#                          RHQ_AGENT_JAVA_EXE_FILE_PATH is set.
#                          If this and RHQ_AGENT_JAVA_EXE_FILE_PATH are
#                          not set, the agent's embedded JRE will be used.
#
#RHQ_AGENT_JAVA_HOME=/opt/java

#    RHQ_AGENT_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                   executable to use. If this is set,
#                                   RHQ_AGENT_JAVA_HOME is ignored.
#                                   If this is not set, then
#                                   $RHQ_AGENT_JAVA_HOME/bin/java
#                                   is used. If this and
#                                   RHQ_AGENT_JAVA_HOME are not set, the
#                                   agent's embedded JRE will be used.
#
#RHQ_AGENT_JAVA_EXE_FILE_PATH=/usr/local/bin/java

#    RHQ_AGENT_JAVA_OPTS - Java VM command line options to be
#                          passed into the agent's VM. If this is not defined
#                          this script will pass in a default set of options.
#                          If this is set, it completely overrides the
#                          agent's defaults. If you only want to add options
#                          to the agent's defaults, then you will want to
#                          use RHQ_AGENT_ADDITIONAL_JAVA_OPTS instead.
#
#RHQ_AGENT_JAVA_OPTS="-Xms160M -Xmx160M -Djava.net.preferIPv4Stack=true"

#    RHQ_AGENT_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
#                                     to be passed into the agent's VM. This
#                                     is added to RHQ_AGENT_JAVA_OPTS; it
#                                     is mainly used to augment the agent's
#                                     default set of options. This can be
#                                     left unset if it is not needed.
#
#RHQ_AGENT_ADDITIONAL_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=9797,server=y,suspend=n"

#    RHQ_AGENT_CMDLINE_OPTS - If this is defined, these are the command line
#                             arguments that will be passed to the RHQ Agent.
#                             If this is not defined, the command line
#                             arguments given to this script are passed
#                             through to the RHQ Agent.
#
#RHQ_AGENT_CMDLINE_OPTS=--nonative

#    RHQ_AGENT_IN_BACKGROUND - If this is defined, the RHQ Agent JVM will
#                              be launched in the background (thus causing this
#                              script to exit immediately).  If the value is
#                              something other than "nofile", it will be assumed
#                              to be a full file path which this script will
#                              create and will contain the agent VM's process
#                              pid value. If this is not defined, the VM is
#                              launched in foreground and this script blocks
#                              until the VM exits, at which time this
#                              script will also exit. NOTE: this should only
#                              be used by the rhq-agent-wrapper.sh script.
#                              If you want to launch the agent in the
#                              background, use that script to do so instead of
#                              setting this variable.
#
#RHQ_AGENT_IN_BACKGROUND=rhq-agent.pid

#===========================================================================
# THE FOLLOWING ARE USED SOLELY FOR THE rhq-agent-wrapper.sh SCRIPT

#    RHQ_AGENT_PIDFILE_DIR - When rhq-agent-wrapper.sh is used to start
#                            the agent, it runs the process in background
#                            and writes its pid to a pidfile. The default
#                            location of this pidfile is in the agent's
#                            /bin directory. If you want to have the pidfile
#                            written to another location, set this environment
#                            variable. This value must be a full path to a
#                            directory with write permissions.
#
#RHQ_AGENT_PIDFILE_DIR=/var/run

#    RHQ_AGENT_RUN_AS - if defined, then the background process will
#                       be run as the given user via "su".
#
#RHQ_AGENT_RUN_AS=$USER

#    RHQ_AGENT_RUN_AS_ME - if defined, then the background process will
#                          be run as the current user (as defined
#                          by the $USER environment variable) via "su".
#                          This takes precedence over RHQ_AGENT_RUN_AS.
#
#RHQ_AGENT_RUN_AS_ME=true

#    RHQ_AGENT_PASSWORD_PROMPT - if "true", this indicates that the user
#                                that is to run the agent must type the
#                                password on the console in order to run.
#                                Therefore, "true" forces a prompt message
#                                to appear on the console. If this is
#                                defined, but not equal to "true", the value
#                                itself will be used as the prompt string. 
#                                This is not defined by default.
#
#RHQ_AGENT_PASSWORD_PROMPT=true

#    RHQ_AGENT_RUN_PREFIX - If defined, this will prefix the agent
#                           start command when it is executed. Use
#                           this to customize how the agent process
#                           is started (e.g. with "su" or "sudo").
#                           If this is set, it completely overrides the
#                           RHQ_AGENT_RUN_AS and RHQ_AGENT_RUN_AS_ME
#                           variables. Note that you can still pipe
#                           the password if you set the RHQ_AGENT_PASSWORD
#                           and RHQ_AGENT_PASSWORD_PROMPT variables properly.
#                           The command to start the agent will be appended
#                           to the end of this prefix string.
#RHQ_AGENT_RUN_PREFIX=su -m - ${RHQ_AGENT_RUN_AS} -c

#    RHQ_AGENT_RUN_PREFIX_QUOTED - If "true", this will wrap the agent
#                                  start command with single quotes
#                                  before being appended to the run prefix.
#                                  If this is defined but not "true", its
#                                  value will be used to wrap the start command.
#                                  (allowing for a value such as \"). 
#                                  This is ignored if RHQ_AGENT_RUN_PREFIX
#                                  is not defined - the purpose of this value
#                                  is to wrap the command so it can look like
#                                  a single argument to the run prefix.
#RHQ_AGENT_RUN_PREFIX_QUOTED=true
#===========================================================================
# RHQ Agent UNIX Startup Script Configuration File
#===========================================================================
#

#    RHQ_AGENT_DEBUG - If this is defined, the script will emit debug
#                      messages. It will also enable debug
#                      messages to be emitted from the agent itself.
#                      If not set or set to "false", debug is turned off.
#
#RHQ_AGENT_DEBUG=true

#    RHQ_AGENT_HOME - Defines where the agent's home install directory is.
#                     If not defined, it will be assumed to be the parent
#                     directory of the directory where this script lives.
#
#RHQ_AGENT_HOME="/opt/rhq-agent" 

#    RHQ_AGENT_JAVA_HOME - The location of the JRE that the agent will
#                          use. This will be ignored if
#                          RHQ_AGENT_JAVA_EXE_FILE_PATH is set.
#                          If this and RHQ_AGENT_JAVA_EXE_FILE_PATH are
#                          not set, the agent's embedded JRE will be used.
#
#RHQ_AGENT_JAVA_HOME="/opt/java"

#    RHQ_AGENT_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                   executable to use. If this is set,
#                                   RHQ_AGENT_JAVA_HOME is ignored.
#                                   If this is not set, then
#                                   $RHQ_AGENT_JAVA_HOME/bin/java
#                                   is used. If this and
#                                   RHQ_AGENT_JAVA_HOME are not set, the
#                                   agent's embedded JRE will be used.
#
#RHQ_AGENT_JAVA_EXE_FILE_PATH="/usr/local/bin/java"

#    RHQ_AGENT_JAVA_OPTS - Java VM command line options to be
#                          passed into the agent's VM. If this is not defined
#                          this script will pass in a default set of options.
#                          If this is set, it completely overrides the
#                          agent's defaults. If you only want to add options
#                          to the agent's defaults, then you will want to
#                          use RHQ_AGENT_ADDITIONAL_JAVA_OPTS instead.
#
#RHQ_AGENT_JAVA_OPTS="-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true"

#    RHQ_AGENT_JAVA_ENDORSED_DIRS - Java VM command line option to set the
#                                   endorsed dirs for the agent's VM. If this
#                                   is not defined this script will pass in a
#                                   default value. If this is set, it
#                                   completely overrides the agent's default.
#                                   However, if this is set to "none", the
#                                   agent will not be passed the VM argument
#                                   to set the endorsed dirs.
#
#RHQ_AGENT_JAVA_ENDORSED_DIRS="${RHQ_AGENT_HOME}/lib/endorsed"

#    RHQ_AGENT_JAVA_LIBRARY_PATH - The RHQ Agent has a JNI library that
#                                  it needs to find in order to do things
#                                  like execute PIQL queries and access
#                                  low-level operating system data. This
#                                  is the native system layer (SIGAR).
#                                  If you deploy a custom plugin that also
#                                  requires JNI libraries, you must add to
#                                  the library path here, but you must ensure
#                                  not to remove the RHQ Agent library path.
#                                  If this variable is set, it completely
#                                  overrides the agent's default.
#                                  However, if this is set to "none", the
#                                  agent will not be passed the VM argument
#                                  to set the library paths.
#
#RHQ_AGENT_JAVA_LIBRARY_PATH="${RHQ_AGENT_HOME}/lib"

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
#                             Any arguments specified on the command line will
#                             be ignored
#                             If this is not defined, the command line
#                             arguments given to the script are passed
#                             through to the RHQ Agent.
#                             If you want to have command line arguments added to
#                             the arguments specified here append $* 
#                             to the end of this option e.g. ="--daemon $*". In this case
#                             both the command line options and the ones specified here
#                             will be passed to the agent.
# 
#RHQ_AGENT_CMDLINE_OPTS="--daemon --nonative"

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
#RHQ_AGENT_PIDFILE_DIR="/var/run"

#    RHQ_AGENT_START_COMMAND - If defined, this is the command that will be
#                              executed to start the agent.
#                              Use this to customize how the agent process
#                              is started (e.g. with "su" or "sudo").
#                              This completely overrides the command used
#                              to start the agent - you must ensure you
#                              provide a valid command that starts the agent
#                              script 'rhq-agent.sh'
#                              Note that if this start command requires the
#                              user to enter a password, you can show a
#                              prompt to the user if you set the variable
#                              RHQ_AGENT_PASSWORD_PROMPT.
#                              Also note that if your agent install directory
#                              has spaces in its name, you might have to do
#                              some special string manipulation to get the
#                              agent script to start. See below for an
#                              example of how to do this. 
#RHQ_AGENT_START_COMMAND="su -m -l user -c '${RHQ_AGENT_HOME}/bin/rhq-agent.sh'"
#RHQ_AGENT_START_COMMAND="su -m -l user -c '$(echo ${RHQ_AGENT_HOME}|sed 's/ /\\ /')/bin/rhq-agent.sh'"

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

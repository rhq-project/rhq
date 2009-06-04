#===========================================================================
# RHQ CLI UNIX Startup Script Configuration File
#===========================================================================
#

#    RHQ_CLI_DEBUG - If this is defined, the script will emit debug
#                      messages. It will also enable debug
#                      messages to be emitted from the cli itself.
#                      If not set or set to "false", debug is turned off.
#
#RHQ_CLI_DEBUG=true

#    RHQ_CLI_HOME - Defines where the cli's home install directory is.
#                     If not defined, it will be assumed to be the parent
#                     directory of the directory where this script lives.
#
#RHQ_CLI_HOME="/opt/rhq-client" 

#    RHQ_CLI_JAVA_HOME - The location of the JRE that the CLI will
#                          use. This will be ignored if
#                          RHQ_CLI_JAVA_EXE_FILE_PATH is set.
#                          If this and RHQ_CLI_JAVA_EXE_FILE_PATH are
#                          not set, the CLI's embedded JRE will be used.
#
#RHQ_CLI_JAVA_HOME="/opt/java"

#    RHQ_CLI_JAVA_EXE_FILE_PATH - Defines the full path to the Java
#                                   executable to use. If this is set,
#                                   RHQ_CLI_JAVA_HOME is ignored.
#                                   If this is not set, then
#                                   $RHQ_CLI_JAVA_HOME/bin/java
#                                   is used. If this and
#                                   RHQ_CLI_JAVA_HOME are not set, the
#                                   CLI's embedded JRE will be used.
#
#RHQ_CLI_JAVA_EXE_FILE_PATH="/usr/local/bin/java"

#    RHQ_CLI_JAVA_OPTS - Java VM command line options to be
#                          passed into the CLI's VM. If this is not defined
#                          this script will pass in a default set of options.
#                          If this is set, it completely overrides the
#                          CLI's defaults. If you only want to add options
#                          to the CLI's defaults, then you will want to
#                          use RHQ_CLI_ADDITIONAL_JAVA_OPTS instead.
#
#RHQ_CLI_JAVA_OPTS="-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true"

#    RHQ_CLI_JAVA_ENDORSED_DIRS - Java VM command line option to set the
#                                   endorsed dirs for the CLI's VM. If this
#                                   is not defined this script will pass in a
#                                   default value. If this is set, it
#                                   completely overrides the CLI's default.
#                                   However, if this is set to "none", the
#                                   CLI will not be passed the VM argument
#                                   to set the endorsed dirs.
#
#RHQ_CLI_JAVA_ENDORSED_DIRS="${RHQ_CLI_HOME}/lib/endorsed"

#    RHQ_CLI_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
#                                     to be passed into the CLI's VM. This
#                                     is added to RHQ_CLI_JAVA_OPTS; it
#                                     is mainly used to augment the CLI's
#                                     default set of options. This can be
#                                     left unset if it is not needed.
#
#RHQ_CLI_ADDITIONAL_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=9787,server=y,suspend=n"

#    RHQ_CLI_CMDLINE_OPTS - If this is defined, these are the command line
#                             arguments that will be passed to the RHQ CLI.
#                             Any arguments specified on the command line
#                             will be ignored. If this is not defined, the
#                             command line arguments given to the script are
#                             passed through to the RHQ CLI.
#                             If you want to have command line arguments
#                             added to the arguments specified here, append
#                             '$*' to the end of this option. For example,
#                             "--someOption $*". In this case, both the command
#                             line options and the ones specified here will
#                             be passed to the CLI.
# 
#RHQ_CLI_CMDLINE_OPTS=""

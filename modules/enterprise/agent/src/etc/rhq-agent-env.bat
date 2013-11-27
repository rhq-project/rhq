rem ===========================================================================
rem RHQ Agent Windows Startup Script Configuration File
rem ===========================================================================
rem

rem    RHQ_AGENT_DEBUG - If this is defined, the script will emit debug
rem                      messages. It will also enable debug
rem                      messages to be emitted from the agent itself.
rem                      If not set or set to "false", debug is turned off.
rem                      This does not implicitly enable Sigar native system
rem                      debug mode.  You must explicitly enable
rem                      RHQ_AGENT_SIGAR_DEBUG in addition to enabling
rem                      RHQ_AGENT_DEBUG for Sigar logging to be enabled.
rem
rem set RHQ_AGENT_DEBUG=true

rem    RHQ_AGENT_SIGAR_DEBUG - Enables Sigar debug mode but only if agent debug
rem                            is also enabled. See RHQ_AGENT_DEBUG for more.
rem set RHQ_AGENT_SIGAR_DEBUG=false

rem    RHQ_AGENT_HOME - Defines where the agent's home install directory is.
rem                     If not defined, it will be assumed to be the parent
rem                     directory of the directory where this script lives.
rem
rem set RHQ_AGENT_HOME=C:\opt\rhq-agent

rem    RHQ_JAVA_HOME - The location of the JRE that the agent will
rem                    use. This will be ignored if
rem                    RHQ_JAVA_EXE_FILE_PATH is set.
rem                    If this and RHQ_JAVA_EXE_FILE_PATH are
rem                    not set, JAVA_HOME will be used.
rem
rem set RHQ_JAVA_HOME=

rem    RHQ_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                             executable to use. If this is set,
rem                             RHQ_JAVA_HOME is ignored.
rem                             If this is not set, then
rem                             %RHQ_JAVA_HOME%\bin\java.exe
rem                             is used. If this and
rem                             RHQ_JAVA_HOME are not set, then
rem                             JAVA_HOME will be used.
rem
rem set RHQ_JAVA_EXE_FILE_PATH=

rem ===========================================================================
rem THE FOLLOWING ARE USED SOLELY FOR THE rhq-agent.bat SCRIPT,
rem THEY ARE IGNORED BY rhq-agent-wrapper.bat SCRIPT. See the
rem   rhq-agent-wrapper.conf and rhq-agent-wrapper.inc for more
rem   on setting Java options.

rem    RHQ_AGENT_JAVA_OPTS - Java VM command line options to be
rem                          passed into the agent's VM. If this is not defined
rem                          this script will pass in a default set of options.
rem                          If this is set, it completely overrides the
rem                          agent's defaults. If you only want to add options
rem                          to the agent's defaults, then you will want to
rem                          use RHQ_AGENT_ADDITIONAL_JAVA_OPTS instead.
rem
rem                          NOTE: This variable is only used by
rem                          rhq-agent.bat; it is NOT used by
rem                          rhq-agent-wrapper.bat. The Java VM command line
rem                          options used by rhq-agent-wrapper.bat are set in
rem                          wrapper\rhq-agent-wrapper.conf.
rem
rem set RHQ_AGENT_JAVA_OPTS=-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true -Drhq.preferences.file=%RHQ_AGENT_HOME%\conf\agent-prefs.properties

rem    RHQ_AGENT_JAVA_ENDORSED_DIRS - Java VM command line option to set the
rem                                   endorsed dirs for the agent's VM. If this
rem                                   is not defined this script will pass in a
rem                                   default value. If this is set, it
rem                                   completely overrides the agent's default.
rem                                   However, if this is set to "none", the
rem                                   agent will not be passed the VM argument
rem                                   to set the endorsed dirs.
rem
rem set RHQ_AGENT_JAVA_ENDORSED_DIRS=%RHQ_AGENT_HOME%\lib\endorsed

rem    RHQ_AGENT_JAVA_LIBRARY_PATH - The RHQ Agent has a JNI library that
rem                                  it needs to find in order to do things
rem                                  like execute PIQL queries and access
rem                                  low-level operating system data. This
rem                                  is the native system layer (SIGAR).
rem                                  If you deploy a custom plugin that also
rem                                  requires JNI libraries, you must add to
rem                                  the library path here, but you must ensure
rem                                  not to remove the RHQ Agent library path.
rem                                  If this variable is set, it completely
rem                                  overrides the agent's default.
rem                                  However, if this is set to "none", the
rem                                  agent will not be passed the VM argument
rem                                  to set the library paths.
rem
rem set RHQ_AGENT_JAVA_LIBRARY_PATH=%RHQ_AGENT_HOME%\lib

rem    RHQ_AGENT_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
rem                                     to be passed into the agent's VM. This
rem                                     is added to RHQ_AGENT_JAVA_OPTS; it
rem                                     is mainly used to augment the agent's
rem                                     default set of options. This can be
rem                                     left unset if it is not needed.
rem
rem                                     NOTE: This variable is only used by
rem                                     rhq-agent.bat; it is NOT used by
rem                                     rhq-agent-wrapper.bat. The Java VM
rem                                     command line options used by
rem                                     rhq-agent-wrapper.bat are set in
rem                                     wrapper\rhq-agent-wrapper.conf.
rem
rem set RHQ_AGENT_ADDITIONAL_JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=9797,server=y,suspend=n

rem    RHQ_AGENT_CMDLINE_OPTS - If this is defined, these are the command line
rem                             arguments that will be passed to the RHQ Agent.
rem                             Any arguments specified on the command line
rem                             will be ignored. If this is not defined, the
rem                             command line arguments given to the script are
rem                             passed through to the RHQ Agent.
rem                             If you want to have command line arguments
rem                             added to the arguments specified here, append
rem                             '%*' to the end of this option. For example,
rem                             "--nonative %*". In this case, both the command
rem                             line options and the ones specified here will
rem                             be passed to the Agent. Note, the --daemon option
rem                             should generally not be included here, since it
rem                             would cause the Agent to always start in daemon
rem                             mode, when started via rhq-agent.bat.
rem
rem                             NOTE: This variable is only used by
rem                             rhq-agent.bat; it is NOT used by
rem                             rhq-agent-wrapper.bat. The Agent command line
rem                             options used by rhq-agent-wrapper.bat are set in
rem                             wrapper\rhq-agent-wrapper.conf.
rem
rem set RHQ_AGENT_CMDLINE_OPTS=--nonative %*

rem ===========================================================================
rem THE FOLLOWING ARE USED SOLELY FOR THE rhq-agent-wrapper.bat SCRIPT

rem    RHQ_AGENT_INSTANCE_NAME - The name of the Windows Service; it must
rem                              conform to the Windows Service naming
rem                              conventions. By default, this is the
rem                              name "rhqagent-%COMPUTERNAME%"
rem
rem set RHQ_AGENT_INSTANCE_NAME=rhqagent-%COMPUTERNAME%

rem    RHQ_AGENT_WRAPPER_LOG_DIR_PATH - The full path to the location where
rem                                     the wrapper log file will go.
rem
rem set RHQ_AGENT_WRAPPER_LOG_DIR_PATH=%RHQ_AGENT_HOME%\logs

rem    RHQ_AGENT_RUN_AS - if defined, then when the Windows Service is
rem                       installed, the value is the domain\username of the
rem                       user that the Windows Service will run as
rem
rem set RHQ_AGENT_RUN_AS=.\%USERNAME%

rem    RHQ_AGENT_RUN_AS_ME - if defined, then when the Windows Service is
rem                          installed, the domain\username of the
rem                          user that the Windows Service will run as will
rem                          be the current user (.\%USERNAME%). This takes
rem                          precedence over RHQ_AGENT_RUN_AS.
rem
rem set RHQ_AGENT_RUN_AS_ME=true

rem    RHQ_AGENT_PASSWORD_PROMPT - ignored unless RHQ_AGENT_RUN_AS or
rem                                RHQ_AGENT_RUN_AS_ME is in use.
rem                                Ignored by 'rhqctl install', in which case
rem                                RHQ_AGENT_PASSWORD is required.
rem                                Used only when installing the agent as a
rem                                service via 'rhq-agent-wrapper.bat install'.
rem                                If "true", the user that is to
rem                                run the service must type the password
rem                                on the console in order to install service.
rem                                If "false", you must provide the password
rem                                in RHQ_AGENT_PASSWORD. Default is "true".
rem                                It is not needed to be set when starting,
rem                                stopping or removing the service.
rem
rem set RHQ_AGENT_PASSWORD_PROMPT=true

rem    RHQ_AGENT_PASSWORD - if RHQ_AGENT_PASSWORD_PROMPT is "false", this
rem                         is the password of the user that is to
rem                         run the service. If RHQ_AGENT_PASSWORD_PROMPT
rem                         is undefined or "true", this is ignored.
rem                         This is only used when installing the
rem                         service; it is not needed to be set when
rem                         starting, stopping or removing the service.
rem
rem set RHQ_AGENT_PASSWORD=password_here

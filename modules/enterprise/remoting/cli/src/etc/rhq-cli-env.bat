rem ===========================================================================
rem RHQ CLI Windows Startup Script Configuration File
rem ===========================================================================
rem

rem    RHQ_CLI_DEBUG - If this is defined, the script will emit debug
rem                      messages. It will also enable debug
rem                      messages to be emitted from the CLI itself.
rem                      If not set or set to "false", debug is turned off.
rem
rem set RHQ_CLI_DEBUG=true

rem    RHQ_CLI_HOME - Defines where the CLI's home install directory is.
rem                     If not defined, it will be assumed to be the parent
rem                     directory of the directory where this script lives.
rem
rem set RHQ_CLI_HOME=C:\opt\rhq-cli

rem    RHQ_JAVA_HOME - The location of the JRE that the server will use. This
rem                    will be ignored if RHQ_JAVA_EXE_FILE_PATH is set.
rem                    If this and RHQ_JAVA_EXE_FILE_PATH are not set, then
rem                    JAVA_HOME will be used.
rem set RHQ_JAVA_HOME=

rem    RHQ_JAVA_EXE_FILE_PATH - Defines the full path to the Java executable to
rem                             use. If this is set, RHQ_JAVA_HOME is ignored.
rem                             If this is not set, then $RHQ_JAVA_HOME/bin/java
rem                             is used. If this and RHQ_JAVA_HOME are not set,
rem                             then $JAVA_HOME/bin/java will be used.
rem set RHQ_JAVA_EXE_FILE_PATH=

rem    RHQ_CLI_JAVA_OPTS - Java VM command line options to be
rem                          passed into the CLI's VM. If this is not defined
rem                          this script will pass in a default set of options.
rem                          If this is set, it completely overrides the
rem                          CLI's defaults. If you only want to add options
rem                          to the CLI's defaults, then you will want to
rem                          use RHQ_CLI_ADDITIONAL_JAVA_OPTS instead.
rem
rem set RHQ_CLI_JAVA_OPTS=-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true -Drhq.scripting.modules.root-dir="%RHQ_CLI_MODULES_DIR%"

rem    RHQ_CLI_JAVA_ENDORSED_DIRS - Java VM command line option to set the
rem                                   endorsed dirs for the CLI's VM. If this
rem                                   is not defined this script will pass in a
rem                                   default value. If this is set, it
rem                                   completely overrides the CLI's default.
rem                                   However, if this is set to "none", the
rem                                   CLI will not be passed the VM argument
rem                                   to set the endorsed dirs.
rem
rem set RHQ_CLI_JAVA_ENDORSED_DIRS=%RHQ_CLI_HOME%\lib\endorsed

rem    RHQ_CLI_ADDITIONAL_JAVA_OPTS - additional Java VM command line options
rem                                     to be passed into the CLI's VM. This
rem                                     is added to RHQ_CLI_JAVA_OPTS; it
rem                                     is mainly used to augment the CLI's
rem                                     default set of options. This can be
rem                                     left unset if it is not needed.
rem
rem set RHQ_CLI_ADDITIONAL_JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=9787,server=y,suspend=n
rem set RHQ_CLI_ADDITIONAL_JAVA_OPTS=-Dorg.jboss.remoting.keyStore=data/keystore.dat -Dorg.jboss.remoting.keyStoreAlgorithm=SunX509 -Dorg.jboss.remoting.keyStoreType=JKS -Dorg.jboss.remoting.keyStorePassword=password -Dorg.jboss.remoting.keyPassword=password -Dorg.jboss.remoting.keyAlias=self

rem    RHQ_CLI_CMDLINE_OPTS - If this is defined, these are the command line
rem                             arguments that will be passed to the RHQ CLI.
rem                             Any arguments specified on the command line
rem                             will be ignored. If this is not defined, the
rem                             command line arguments given to the script are
rem                             passed through to the RHQ CLI.
rem                             If you want to have command line arguments
rem                             added to the arguments specified here, append
rem                             '%*' to the end of this option. For example,
rem                             "--someOption %*". In this case, both the command
rem                             line options and the ones specified here will
rem                             be passed to the CLI.
rem
rem set RHQ_CLI_CMDLINE_OPTS=

rem    RHQ_CLI_CHANGE_DIR_ON_START - By setting this variable to true (or any
rem                                  other value than "false") you can make RHQ
rem                                  change the directory to $RHQ_CLI_HOME when 
rem                                  starting the CLI. When this variable is set 
rem                                  to false, the current working directory is
rem                                  NOT changed when starting the CLI.
rem
rem                                  If not set, this variable is understood
rem                                  to be: ${rhq.cli.change-dir-on-start-default}
rem set RHQ_CLI_CHANGE_DIR_ON_START=true

rem    RHQ_CLI_MODULES_DIR - The default location from which to load CommonJS 
rem                          modules available through the "modules:/" scheme
rem                          is $RHQ_CLI_HOME/samples/modules.
rem                          Setting this variable to another value, causes
rem                          the modules to be loaded from another location.
rem                          
rem                          Notice that this can be a relative path, too.
rem                          For example, setting:
rem                          RHQ_CLI_MODULES_DIR=./modules
rem                          would cause the CLI to load modules from the
rem                          "modules" subdirectory of whichever current 
rem                          working directory it is started from.
rem                          (See RHQ_CLI_JAVA_OPTS and 
rem                          RHQ_CLI_CHANGE_DIR_ON_START variables above for 
rem                          further details of this approach).
rem                             
rem set RHQ_CLI_MODULE_DIR=Z:\company-wide\rhq\cli\modules


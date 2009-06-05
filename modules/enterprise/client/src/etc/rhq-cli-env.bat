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

rem    RHQ_CLI_JAVA_HOME - The location of the JRE that the CLI will
rem                          use. This will be ignored if
rem                          RHQ_CLI_JAVA_EXE_FILE_PATH is set.
rem                          If this and RHQ_CLI_JAVA_EXE_FILE_PATH are
rem                          not set, the CLI will default to JAVA_HOME.
rem
rem set RHQ_CLI_JAVA_HOME=

rem    RHQ_CLI_JAVA_EXE_FILE_PATH - Defines the full path to the Java
rem                                   executable to use. If this is set,
rem                                   RHQ_CLI_JAVA_HOME is ignored.
rem                                   If this is not set, then
rem                                   %RHQ_CLI_JAVA_HOME%\bin\java.exe
rem                                   is used.
rem
rem set RHQ_CLI_JAVA_EXE_FILE_PATH=C:\WINDOWS\system32\java.exe

rem    RHQ_CLI_JAVA_OPTS - Java VM command line options to be
rem                          passed into the CLI's VM. If this is not defined
rem                          this script will pass in a default set of options.
rem                          If this is set, it completely overrides the
rem                          CLI's defaults. If you only want to add options
rem                          to the CLI's defaults, then you will want to
rem                          use RHQ_CLI_ADDITIONAL_JAVA_OPTS instead.
rem
rem set RHQ_CLI_JAVA_OPTS=-Xms64m -Xmx128m -Djava.net.preferIPv4Stack=true

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
rem set RHQ_CLI_ADDITIONAL_JAVA_OPTS=-CLIlib:jdwp=transport=dt_socket,address=9797,server=y,suspend=n

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

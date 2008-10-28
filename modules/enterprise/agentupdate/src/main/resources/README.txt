=======================
RHQ Agent Update Binary
=======================

_______________________
DESCRIPTION

This is the RHQ Agent Update Binary jar file.  The purpose of this
jar file is to allow you to install a fresh agent on a box
where an agent does not yet exist and to allow you to update
an agent that is already installed.

This jar file is not meant to be unjar'ed.
It is intended to be run as a "self-executing jar" via the
"java -jar" command.

Note that a running RHQ Agent will be able to auto-update itself
using this jar file, if the RHQ Agent downloaded this file
during its "auto-update-check" routine. If you do not want to
use or rely on the RHQ Agent's auto-update functionality,
you can use this jar file to manually update an existing agent
by following the instructions below.
 
_______________________
VERSION

To determine the version of this agent update binary (which is the same
as the agent version itself), you can pass the "--version" command
line option to the self-executing jar file.

_______________________
USAGE

This is a "self-executing" jar file, meaning you can execute it using
the Java command line:

java -jar <agent-update-binary-filename>

where <agent-update-binary-filename> is the name of the jar file itself.
If you run that exact command, with no other command line arguments
passed to Java, help information will be displayed on the console.

If you run this command:

java -jar <agent-update-binary-filename> --install[=<new-agent-directory>]

a new agent will be installed in the given directory, or your current
working directory if you do not specify a directory.

If you run this command:

java -jar <agent-update-binary-filename> --update[=<old-agent-directory>]

that will update an existing agent located in the directory
specified by <old-agent-directory>, or in the current working directory
if you do not specify a directory.

There are additional command line arguments that can be specified
to the agent update binary jar file; to get full help text, run:

java -jar <agent-update-binary-filename> --help

which is the same as not passing any command line arguments.

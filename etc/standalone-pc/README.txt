Installation:
-------------


Download an agent from the RHQ-server as you would do for real agent operations or
alternatively copy an existing agent directory

Copy the standalone.(sh|bat) script to this agent directory in bin/ next to the
existing rhq-agent.(sh|bat) scripts

If you have no plugins in the plugin/ directory, then copy at least the rhq-platform-*.jar
to plugins/ (e.g. from the rhq.ear/rhq-ownloads/rhq-plugins) directory of the server.

Usage:
------

Copy your own plugin to plugins/

Start the wrapper:  bin/standalone.sh

At the command prompt type 'help' for a list of available commands.
Type '!h' for help about the history functionality.

If you start the script with a file name ( as in bin/standalone.sh inputFile ), this
file is expected to contain a set of commands that are used as input ( "script mode" )

It is possible to start a line with a hash mark (#) which means that this is a comment line.

Also have a look at a video that shows how to use the standalone container:
http://vimeo.com/18624567

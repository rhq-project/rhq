Overview
---------
The scripts located in the samples directory are intended for demonstration
purposes to illustrate functionality and features in the CLI. These scripts
have not undergone the same level of testing as other parts of the code base.
As such, the quality may not meet RHQ standards in some cases. Please consider
this when using these scripts for anything other than demonstration or testing.

There are inter-dependencies between some of the scripts. But because these
scripts are not implemented as modules but rather as examples of what
can be done, those dependencies are not expressed in code but have to be 
expressed manually on the commandline of the interactive CLI as suggested by
the comments in the respective files (another reason is to keep the scripts
backwards compatible in case someone used them in previous versions of RHQ
that didn't offer CommonJS modules).

In the "modules" directory, there are sample modules that mirror many of
these sample scripts in a "modularized" form so that they are more easily
available in your scripts (if you compare the files, you will find that
the changes are fairly minor).

The scripts in the "modules" directory are also available in the downloads
section of the RHQ server. From your scripts, they are therefore available
in at least two ways:

1) var bundles = require("rhq://samples/modules/bundles");
This will load the module stored locally in the samples/modules directory.

2) var util = require("rhq://downloads/util");
This will, as long as the CLI session is connected to an RHQ server, download
the script from the RHQ server.

Note that the latter, i.e. the "rhq://downloads/...", is also usable in the
alert scripts that run on the RHQ server as alert notifications (unlike,
of course, the former form).

Feedback
--------
The CLI is still a relatively new, young feature. We welcome and encourage
feedback. Please send questions, comments, etc. to
rhq-users@lists.fedorahosted.org or and/or to rhq-devel@lists.fedorahosted.org
or use the forums at http://community.jboss.org/en/jopr

Thanks

RHQ development team

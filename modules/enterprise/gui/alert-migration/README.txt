
This module is to migrate alert definitions from rhq 1.3.1 to rhq 3

You need to follow those steps:

- have rhq 1.3.1 running
- dump the old alerts (running the scripts need java 1.6 )

  $ rhq-cli.sh -u rhqadmin -p rhqadmin -f dumpAlertDefinitions.js

  This will dump the alert definitions in to a file called alertDefinitions.csv
  in the current directory

- download rhq 3 and run the installer
- *after* running the installer, direct the
  browser to localhost:7080/alert-migration
  and upload the alertDefinitions.csv file


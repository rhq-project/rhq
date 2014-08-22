# REST-API Integration tests

These require a running server with at least one platform as resource id 10001 in inventory.
Also required are two accounts

* rhqadmin with password rhqadmin in the superuser role
* user with password name23 as a restricted user
* imported RHQ Server resource in inventory
* imported AS7 Standalone resource in inventory

The server to connect to can be given on the command line via `-Drest.server=<server>` as in

  `mvn -Pdev -Drest.server=pintsize test`

If no server is given, localhost is assumed.

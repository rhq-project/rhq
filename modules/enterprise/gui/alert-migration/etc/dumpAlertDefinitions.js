#
# CLI script to export Alert Definitions
# Writes the dump to a file 'alertDefinitions.csv' in the
# current directory - appends to this file if it already exists.
# call it liek this: "rhq-cli.sh -u rhqadmin -p rhqadmin -f dumpAlertDefinitions.js"
#
criteria = new AlertDefinitionCriteria()
criteria.fetchAlertNotifications(true)

defs = AlertDefinitionManager.findAlertDefinitionsByCriteria(subject,criteria)

exporter.setTarget('csv', 'alertDefinitions.csv')
exporter.write(defs)


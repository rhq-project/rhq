rhq.login('rhqadmin', 'rhqadmin');

pretty.print(subject);

exporter.setTarget('raw', 'output.txt');
exporter.write(subject);

rhq.logout();

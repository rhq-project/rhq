When deploying the testsuite war file, also deploy the delete-ds.xml file, found in this directory. 
This is the datasource that gets deleted in the testDelete Unit Test.

When the test has finished, then delete the AnotherTestDS-ds.xml from the deploy directory so that the next run of the 
tests will successfully recreate that datasource. 

If you don't delete the file, it will just over-write it, so you don't really get an error. 

After deployed and AS running the URL to run the test is
http://localhost:8080/jboss-as5-testsuite/ServletTestRunner?suite=org.rhq.plugins.jboss.JBossServerTest

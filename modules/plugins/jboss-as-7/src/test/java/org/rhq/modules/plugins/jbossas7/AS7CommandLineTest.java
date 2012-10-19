package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.util.PropertiesFileUpdate;

/**
 * A unit test for {@link AS7CommandLine}.
 */
@Test
public class AS7CommandLineTest {

    public void testSysProps() throws Exception {
        File propsFile1 = File.createTempFile("jboss1-", ".properties");

        PropertiesFileUpdate propsFile1Updater = new PropertiesFileUpdate(propsFile1.getPath());
        propsFile1Updater.update("prop1", "delta");
        propsFile1Updater.update("prop4", "epsilon");

        File propsFile2 = File.createTempFile("jboss2-", ".properties");
        PropertiesFileUpdate propsFile2Updater = new PropertiesFileUpdate(propsFile1.getPath());
        propsFile2Updater.update("prop2", "zeta");
        propsFile2Updater.update("prop5", "eta");

        AS7CommandLine javaCommandLine = new AS7CommandLine(new String[] { //
                "/usr/java/default/bin/java", //
                "-D[Standalone]", //
                "-server", //
                "-Dprop1=alpha", //
                "-Dprop2=beta", //
                "-Dprop3=gamma", //
                "-jar", "/home/jboss/jboss-eap-6.0.0.ER6/jboss-modules.jar", //
                "-mp", "/home/jboss/jboss-eap-6.0.0.ER6/modules", //
                "-jaxpmodule", "javax.xml.jaxp-provider", //
                "org.jboss.as.standalone", //
                "-Djboss.home.dir=/home/jboss/jboss-eap-6.0.0.ER6", //
                "-Djboss.server.base.dir=/home/jboss/jboss-eap-6.0.0.ER6/standalone", //
                "-P", "file://" + propsFile1, //
                "--properties=file://" + propsFile2 //
        });
        Map<String,String> sysprops = javaCommandLine.getSystemProperties();
        Assert.assertNotNull(sysprops);
        Assert.assertEquals(sysprops.size(), 8);
        Assert.assertEquals(sysprops.get("[Standalone]"), "");
        Assert.assertEquals(sysprops.get("prop1"), "delta");
        Assert.assertEquals(sysprops.get("prop2"), "zeta");
        Assert.assertEquals(sysprops.get("prop3"), "gamma");
        Assert.assertEquals(sysprops.get("prop4"), "epsilon");
        Assert.assertEquals(sysprops.get("prop5"), "eta");
        Assert.assertEquals(sysprops.get("jboss.home.dir"), "/home/jboss/jboss-eap-6.0.0.ER6");
        Assert.assertEquals(sysprops.get("jboss.server.base.dir"), "/home/jboss/jboss-eap-6.0.0.ER6/standalone");
    }

}

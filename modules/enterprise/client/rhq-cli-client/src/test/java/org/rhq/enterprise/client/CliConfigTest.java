package org.rhq.enterprise.client;

import java.io.File;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.enterprise.client.CliConfig.Manager;
import org.rhq.enterprise.client.CliConfig.Parameter;
import org.rhq.enterprise.client.CliConfig.Service;

@Test(groups = "RemoteAPI")
public class CliConfigTest extends AssertJUnit {

    static private final boolean TESTS_ENABLED = true;

    @BeforeMethod
    protected void beforeMethod() throws Exception {
        // nothing
    }

    @AfterMethod
    protected void afterMethod() throws Exception {
        // nothing
    }

    //Create and use SubjectManagerBean
    @Test(enabled = TESTS_ENABLED)
    public void testParsing() throws Exception {
        File configFile = new File("./src/main/resources/cli.xml");
        assertTrue(configFile.exists());

        CliConfig config = CliConfig.getConfig(configFile);
        assertNotNull(config);
        assertNotNull(config.getManagers());
        assertFalse(config.getManagers().isEmpty());

        List<Manager> managers = config.getManagers("ResourceM");
        assertTrue(1 == managers.size());
        Manager resourceManager = managers.get(0);
        assertFalse(resourceManager.getServices().isEmpty());

        List<Service> services = resourceManager.getServices("find");
        assertTrue(2 == services.size());
        services = resourceManager.getServices("findResources");
        assertTrue(1 == services.size());
        Service findResourcesService = services.get(0);

        List<Parameter> parameters = findResourcesService.getParameters();
        assertNotNull(parameters);
        Parameter criteria = findResourcesService.getParameter("criteria");
        assertNotNull(criteria);
        Parameter pageControl = findResourcesService.getParameter("pageControl");
        assertNotNull(pageControl);
        assertNull(findResourcesService.getParameter("doesNotExist"));
    }

}

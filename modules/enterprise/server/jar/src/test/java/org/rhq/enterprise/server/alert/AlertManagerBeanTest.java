package org.rhq.enterprise.server.alert;

import static org.testng.AssertJUnit.assertEquals;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.testng.annotations.Test;

@Test
public class AlertManagerBeanTest {

    public void testPrettyPrintAlertURL() {
        AlertDefinition alertDefinition = new AlertDefinition();
        AlertManagerBean bean = new AlertManagerBean();
        Alert alert = new Alert(alertDefinition, 0);
        Resource r = new Resource(42);
        alertDefinition.setResource(r);
        String base = "http://localhost";
        String url = bean.prettyPrintAlertURL(alert, base);
        assertEquals("http://localhost/coregui/#Resource/" + r.getId()  + "/Alerts/History/0", url);

        alertDefinition.setResource(null);
        ResourceGroup group = new ResourceGroup("foo");
        group.setId(29);
        alertDefinition.setGroup(group);
        url = bean.prettyPrintAlertURL(alert, base);
        assertEquals("http://localhost/coregui/#ResourceGroup/" + group.getId() + "/Alerts/History/0", url);
    }


}

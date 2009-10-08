package org.rhq.enterprise.gui.configuration.test;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

public class ConfigTestUIBean {

    private Configuration configuration;
    private List<Property> properties;

    public ConfigTestUIBean() {
        int configId = FacesContextUtility.getOptionalRequestParameter("configId", Integer.class, -1);
        configuration = LookupUtil.getConfigurationManager().getConfigurationById(configId);
        properties = new ArrayList<Property>();
        // unwrap the hibernate proxy objects, which facelets appears not to be able to handle
        for (Property prop : configuration.getProperties()) {
            properties.add(prop);
        }
    }

    public List<Property> getProperties() {
        return properties;
    }
}

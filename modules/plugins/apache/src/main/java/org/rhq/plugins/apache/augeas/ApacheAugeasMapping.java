package org.rhq.plugins.apache.augeas;

import org.rhq.augeas.AugeasProxy;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.rhqtransform.RhqAugeasMapping;

public class ApacheAugeasMapping implements RhqAugeasMapping {

    public void updateAugeas(AugeasProxy augeasComponent, Configuration config, ConfigurationDefinition configDef) {
     
    }

    public Configuration updateConfiguration(AugeasProxy augeasComponent, ConfigurationDefinition configDef) {
        return null;
    }

}

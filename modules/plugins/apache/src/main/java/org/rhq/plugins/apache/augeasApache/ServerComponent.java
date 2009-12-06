package org.rhq.plugins.apache.augeasApache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.apache.augeas.AugeasConfigurationApache;
import org.rhq.plugins.apache.augeas.AugeasToApacheConfiguration;
import org.rhq.plugins.apache.augeas.AugeasTreeBuilderApache;
import org.rhq.plugins.platform.PlatformComponent;
import org.rhq.rhqtransform.AugeasRHQComponent;

public class ServerComponent implements AugeasRHQComponent<PlatformComponent>, ConfigurationFacet {

    private ResourceContext context;
    private final Log log = LogFactory.getLog(this.getClass());
    private AugeasTree augeasTree;
    private AugeasProxy augeasComponent;

    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        this.context = context;

    }

    public void stop() {
        // TODO Auto-generated method stub

    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void loadAugeas() throws AugeasTreeException {
        AugeasConfigurationApache config = new AugeasConfigurationApache(context.getPluginConfiguration());
        AugeasTreeBuilderApache builder = new AugeasTreeBuilderApache();
        augeasComponent = new AugeasProxy(config, builder);
        augeasComponent.load();
        augeasTree = augeasComponent.getAugeasTree("httpd", true);
    }

    public AugeasProxy getAugeasProxy() throws AugeasTreeException {
        if (augeasComponent == null)
            loadAugeas();

        return augeasComponent;
    }

    public AugeasTree getAugeasTree() throws AugeasTreeException {
        if (augeasTree == null)
            loadAugeas();

        return augeasTree;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration pluginConfiguration = null;
        ConfigurationDefinition resourceConfigDef = this.context.getResourceType().getResourceConfigurationDefinition();
        try {

            AugeasTree tree = getAugeasTree();
            AugeasToApacheConfiguration config = new AugeasToApacheConfiguration();
            config.setTree(tree);

            pluginConfiguration = config.loadResourceConfiguration(tree.getRootNode(), resourceConfigDef);
        } catch (Exception e) {
            log.error(e.getMessage());

        }
        return pluginConfiguration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // TODO Auto-generated method stub

    }
}

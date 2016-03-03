package org.rhq.modules.plugins.wildfly10;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.ReadResource;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Discovery class for ModCluster Domain node. 
 * 
 * @author Simeon Pinder
 */
public class ModClusterDiscoveryComponent implements ResourceDiscoveryComponent<BaseComponent<?>> {

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws InvalidPluginConfigurationException, Exception {

        Set<DiscoveredResourceDetails> details = new HashSet<DiscoveredResourceDetails>();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);

        BaseComponent parentComponent = context.getParentResourceComponent();
        ASConnection connection = parentComponent.getASConnection();

        //load plugin descriptor defaults/config
        Configuration config = context.getDefaultPluginConfiguration();
        String confPath = config.getSimpleValue("path", "");
        if (confPath == null || confPath.isEmpty()) {
            log.error("Path plugin config is null for ResourceType [" + context.getResourceType().getName() + "].");
            return details;
        }

        // Single subsystem
        String path = parentComponent.getPath() + "," + confPath;
        if (path.startsWith(","))
            path = path.substring(1);
        PropertySimple ps = new PropertySimple("path", path);

        //discover the node if it's available.
        Result result = connection.execute(new ReadResource(new Address(path)));
        if (result.isSuccess()) {

            String resKey = path;
            //strip off subsystem
            String name = resKey.substring(resKey.lastIndexOf("=") + 1);
            Configuration config2 = context.getDefaultPluginConfiguration();
            PropertySimple pathProp = new PropertySimple("path", path);
            config2.put(pathProp);

            DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), // DataType
                path, // Key
                name, // Name
                null, // Version
                context.getResourceType().getDescription(), // Description
                config2, null);
            details.add(detail);
        }

        return details;
    }
}

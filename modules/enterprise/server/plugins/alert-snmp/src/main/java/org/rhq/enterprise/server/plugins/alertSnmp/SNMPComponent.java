package org.rhq.enterprise.server.plugins.alertSnmp;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.util.LookupUtil;

public class SNMPComponent implements ServerPluginComponent {

    private static final Log LOG = LogFactory.getLog(SNMPComponent.class);

    public static final String PROP_TRANSPORT = "transport";

    public static final String TRANSPORT_UDP = "UDP";
    public static final String TRANSPORT_TCP = "TCP";
    public static final String DEFAULT_TRANSPORT = TRANSPORT_UDP;


    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        try {
            Configuration configuration = context.getPluginConfiguration();
            if (configuration != null) {
                final List<String> VALID_TRANSPORTS = Arrays.asList(TRANSPORT_UDP, TRANSPORT_TCP);

                String transportValue = configuration.getSimpleValue(PROP_TRANSPORT, null);
                if (transportValue == null || !VALID_TRANSPORTS.contains(transportValue)) {
                    ServerPluginManagerLocal spm = LookupUtil.getServerPluginManager();
                    ServerPlugin serverPlugin  = spm.getServerPlugin(context.getPluginEnvironment().getPluginKey());
                    serverPlugin.getPluginConfiguration().setSimpleValue(PROP_TRANSPORT, DEFAULT_TRANSPORT);
                    LookupUtil.getServerPluginManager().updateServerPluginExceptContent(
                            LookupUtil.getSubjectManager().getOverlord(),
                            serverPlugin
                    );
                }
            }
        } catch(Exception exception) {
            LOG.debug("Exception while trying to fix transport default value", exception);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void shutdown() {

    }
}

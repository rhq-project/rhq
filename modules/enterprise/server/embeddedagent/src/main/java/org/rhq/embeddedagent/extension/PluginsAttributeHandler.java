package org.rhq.embeddedagent.extension;

import java.util.HashMap;
import java.util.List;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceNotFoundException;

class PluginsAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public static final PluginsAttributeHandler INSTANCE = new PluginsAttributeHandler();

    private final Logger log = Logger.getLogger(PluginsAttributeHandler.class);

    private PluginsAttributeHandler() {
        super(AgentSubsystemDefinition.PLUGINS_ATTRIBDEF);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
        ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder)
        throws OperationFailedException {

        log.debug("Embedded agent plugins attribute changed: " + attributeName + "=" + resolvedValue);
        setPluginsWithEnabledFlag(context, resolvedValue);
        return true; // the service must be restarted to really pick up the change at runtime
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
        ModelNode valueToRestore, ModelNode valueToRevert, Void handback) {

        log.debug("Reverting embedded agent plugins attribute: " + attributeName + "=" + valueToRestore);
        setPluginsWithEnabledFlag(context, valueToRestore);
    }

    private void setPluginsWithEnabledFlag(OperationContext context, ModelNode newValue) {

        try {
            AgentService service = (AgentService) context.getServiceRegistry(true)
                .getRequiredService(AgentService.SERVICE_NAME).getValue();

            HashMap<String, Boolean> pluginsWithEnableFlag = new HashMap<String, Boolean>();
            if (newValue != null && newValue.isDefined()) {
                List<Property> pluginsList = newValue.asPropertyList();
                for (Property pluginsItem : pluginsList) {
                    String pluginName = pluginsItem.getName();
                    boolean pluginEnabled = pluginsItem.getValue().asBoolean();
                    pluginsWithEnableFlag.put(pluginName, pluginEnabled);
                }
            }
            service.setPlugins(pluginsWithEnableFlag);
        } catch (ServiceNotFoundException snfe) {
            // the agent is probably disabled or undeployed, don't bother doing anything
        }
    }
}

package org.rhq.plugins.byteman;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.byteman.agent.submit.Submit;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Component that represents an individual rule found within a script that is deployed in a Byteman agent.
 * 
 * @author John Mazzitelli
 */
public class BytemanRuleComponent implements ResourceComponent<BytemanScriptComponent> {

    private final Log log = LogFactory.getLog(BytemanRuleComponent.class);

    private ResourceContext<BytemanScriptComponent> resourceContext;

    public void start(ResourceContext<BytemanScriptComponent> context) {
        this.resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        try {
            String ourKey = this.resourceContext.getResourceKey();
            Submit client = this.resourceContext.getParentResourceComponent().getBytemanClient();
            List<String> rules = this.resourceContext.getParentResourceComponent().getRules();
            for (String rule : rules) {
                String ruleName = client.determineRuleName(rule);
                if (ourKey.equals(ruleName)) {
                    return AvailabilityType.UP;
                }
            }
            return AvailabilityType.DOWN;
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }
}

package org.rhq.plugins.apache;

import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;

public interface ApacheConfigurationBase<T extends ResourceComponent> extends ResourceComponent<T> { 
    public abstract ApacheDirective getNode(ApacheDirectiveTree tree);
    public abstract ApacheDirectiveTree loadParser();   
    public abstract boolean saveParser(ApacheDirectiveTree tree);
    public void conditionalRestart() throws Exception;
}

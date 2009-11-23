package org.rhq.plugins.iptables;

import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

public interface AugeasRHQComponent <T extends ResourceComponent> extends ResourceComponent<T>{
   
	public AugeasTree getAugeasTree() throws Exception;
    public AugeasComponent getAugeasComponent() throws Exception;
}


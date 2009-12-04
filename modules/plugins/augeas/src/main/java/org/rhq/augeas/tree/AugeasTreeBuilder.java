package org.rhq.augeas.tree;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;

public interface AugeasTreeBuilder {

	public AugeasTree buildTree(AugeasProxy component,AugeasConfiguration moduleConfig,
			                  String name,boolean lazy) throws Exception;
}

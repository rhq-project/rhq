package org.rhq.plugins.apache.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.RhqConfig;

public class AugaesConfigurationApache extends RhqConfig implements AugeasConfiguration{

	
	public static String INCLUDE_DIRECTIVE= "";
	private AugeasModuleConfig module;
	
	public AugaesConfigurationApache(Configuration configuration) throws AugeasRhqException{
		super(configuration);
		
		if (modules.isEmpty())
			throw new AugeasRhqException("There is not configuration for this resource.");
		try {
		 module = modules.get(0);
		
		updateIncludes();
		}catch(Exception e){
			throw new AugeasRhqException(e.getMessage());
		}
	}
	
	public List<String> getIncludes(File file){
		List<String> includeFiles = new ArrayList<String>();
		
		return includeFiles;
	}
	
	public void updateIncludes() throws Exception{
			
		
		boolean updated = false;
		AugeasProxy augeas = new AugeasProxy(this);
		augeas.load();
		
		AugeasTree tree = augeas.getAugeasTree("httpd", true);
		
		AugeasNode nd = tree.getRootNode();
		List<AugeasNode> nds = nd.getChildNodes();
	
		for (AugeasNode ns : nds)
		{
		List<AugeasNode> nodes = tree.matchRelative(ns,"/Include");
		for (AugeasNode node : nodes)
		{
			String value = node.getValue();
			if (!module.getIncludedGlobs().contains(value))
			{
				module.addIncludedGlob(value);
				updated = true;
			}
		}
		}
		if (updated)
			updateIncludes();
	}
}

package org.rhq.rhqtransform;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;

public class RhqTransform {
	
	private final Log log = LogFactory.getLog(this.getClass());
    private AugeasTree tree;
    
	public RhqTransform(AugeasTree tree){
	 this.tree = tree;	
	}
		  
	
	public Configuration updateConfiguration(){
		return null;
	}
	
	public AugeasTree updateAugeas(){
		return null;
	}
}

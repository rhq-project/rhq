/**
 * 
 */
package org.rhq.plugins.jbossas5.util;

import org.jboss.on.common.jbossas.AbstractJBossASContentFacetDelegate;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;

/**
 * Specialization of the content facet delegate for JBoss AS 5.
 * 
 * @author Lukas Krejci
 */
public class JBossASContentFacetDelegate extends
		AbstractJBossASContentFacetDelegate {

	public JBossASContentFacetDelegate(JBPMWorkflowManager workflowManager) {
		super(workflowManager);
	}

	//TODO what exactly do we need reimplemented here? Come back here once we have
	//support for operations in ApplicationServerComponent so that we can finish
	//the content support.
}

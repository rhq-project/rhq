package org.rhq.sample.perspective;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 *
 */
@Name("BrowseResources2UIBean")
@Scope(ScopeType.CONVERSATION)
public class BrowseResources2UIBean extends BrowseResourcesUIBean {
}

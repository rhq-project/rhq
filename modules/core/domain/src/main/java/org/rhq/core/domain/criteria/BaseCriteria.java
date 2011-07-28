package org.rhq.core.domain.criteria;

import org.rhq.core.domain.util.PageControl;

/**
 * All criteria, regardless of the backend storage that will be queried with this criteria, needs
 * to support certain base functionality (like paging).
 * This base interface provides that common API.
 * 
 * @author John Sanda
 */
public interface BaseCriteria {

    PageControl getPageControlOverrides();

    void setPageControl(PageControl pageControl);

}

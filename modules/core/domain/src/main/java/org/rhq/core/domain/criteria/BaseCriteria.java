package org.rhq.core.domain.criteria;

import org.rhq.core.domain.util.PageControl;

/**
 * Created by IntelliJ IDEA. User: jsanda Date: 7/19/11 Time: 5:30 PM To change this template use File | Settings | File
 * Templates.
 */
public interface BaseCriteria {

    PageControl getPageControlOverrides();

    void setPageControl(PageControl pageControl);

}

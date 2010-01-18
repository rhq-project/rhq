package org.rhq.core.gui.model;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * A class that is able to return a paged, and optionally sorted, set of domain objects of type {@link T}}. Paging and
 * sorting are done as specified by a provided {@link PageControl page control}.
 *
 * @param <T> the domain object type (e.g. org.rhq.core.domain.Resource)
 */
public interface PagedDataProvider<T> {
    /**
     * @param pageControl the page control that specifies which page to fetch and what field(s) to sort by
     *
     * @return List<T> the requested page, which also includes the total size of the data set
     */
    PageList<T> getDataPage(PageControl pageControl);
}

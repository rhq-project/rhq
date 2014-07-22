/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.domain.criteria;

import java.util.List;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;

/**
 * All criteria, regardless of the backend storage that will be queried with this criteria, needs
 * to support certain base functionality (like paging).  This base interface provides that common API.
 *
 * @author John Sanda
 */
public interface BaseCriteria {

    /**
     * @param sortId
     * @since 4.7
     */
    void addSortId(PageOrdering sortId);

    /**
     * @return
     * @since 4.7
     */
    List<String> getOrderingFieldNames();

    /**
     * @return
     */
    PageControl getPageControlOverrides();

    /**
     * @param pageControl
     */
    void setPageControl(PageControl pageControl);

    /**
     * @param strict
     */
    void setStrict(boolean strict);

    /**
     * @return
     */
    public boolean isStrict();

    /**
     * @param pageNumber
     * @param pageSize
     */
    public void setPaging(int pageNumber, int pageSize) ;

}

/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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

/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.common.paging;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceNamesDisambiguationResult;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is an extension to the {@link PagedListDataModel} that automatically
 * performs the disambiguation of the resource names contained in the pages of fetched data.
 * <p> 
 * This class implements the {@link PagedListDataModel#fetchPage(PageControl)} method and defers
 * the actual loading of the data to a new {@link #fetchDataForPage(PageControl)} method. The result
 * of that call is supplied to the {@link ResourceManagerLocal#disambiguate(java.util.List, boolean, IntExtractor)}
 * method and the disambiguated results are then returned from the {@link #fetchPage(PageControl)} method.
 * 
 * @author Lukas Krejci
 */
public abstract class ResourceNameDisambiguatingPagedListDataModel<T> extends
    PagedListDataModel<DisambiguationReport<T>> {

    private boolean alwaysIncludeParents;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    /**
     * @param view
     * @param beanName
     * @param alwaysIncludeParents whether the disambiguation should always include the parent
     * names even if they wouldn't be needed to make the resource names unique.
     */
    public ResourceNameDisambiguatingPagedListDataModel(PageControlView view, String beanName,
        boolean alwaysIncludeParents) {
        super(view, beanName);
        this.alwaysIncludeParents = alwaysIncludeParents;
    }

    public PageList<DisambiguationReport<T>> fetchPage(PageControl pc) {
        PageList<T> data = fetchDataForPage(pc);

        ResourceNamesDisambiguationResult<T> disambiguation = resourceManager.disambiguate(data, alwaysIncludeParents,
            getResourceIdExtractor());

        return new PageList<DisambiguationReport<T>>(disambiguation.getResolution(), data.getTotalSize(), data
            .getPageControl());
    }

    /**
     * This method is to be implemented by inheritors and  is called to fetch the actual data
     * that contain the resources to disambiguate.
     * 
     * @param pc
     * @return
     */
    protected abstract PageList<T> fetchDataForPage(PageControl pc);

    /**
     * @return an extractor for getting a resource id out of the instance of type T.
     */
    protected abstract IntExtractor<T> getResourceIdExtractor();
}

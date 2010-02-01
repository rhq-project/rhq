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
package org.rhq.core.gui.table.bean;

import org.jboss.seam.annotations.In;
import org.jboss.seam.faces.FacesMessages;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.UnlimitedPageControl;
import org.rhq.core.gui.table.model.PagedListDataModel;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

/**
 * @author Ian Springer
 */
public abstract class AbstractPagedDataUIBean<T> {
    private static final int[] PAGE_SIZES = new int[] {15, 30, 45};
    private static final SelectItem[] PAGE_SIZE_SELECT_ITEMS = new SelectItem[PAGE_SIZES.length];
    static {
        for (int i = 0; i < PAGE_SIZES.length; i++) {
            PAGE_SIZE_SELECT_ITEMS[i] = new SelectItem(PAGE_SIZES[i], Integer.valueOf(PAGE_SIZES[i]).toString());
        }
    }
    private static final int MINIMUM_PAGE_SIZE = PAGE_SIZES[0];
    private static final int DEFAULT_PAGE_SIZE = PAGE_SIZES[0];

    /** The number of specific pages to display on the data scroller - limit to 7 pages until we find a general fix for
     *  RHQ-1813. */
    private static final int DATA_SCROLLER_MAX_PAGES = 7;

    @In
    protected FacesMessages facesMessages;
    
    private PageControl pageControl;
    private PagedListDataModel<T> dataModel;

    public SelectItem[] getPageSizeSelectItems() {
        return PAGE_SIZE_SELECT_ITEMS;
    }

    public int getPageSizeCount() {
        return PAGE_SIZES.length;
    }

    public int getMinimumPageSize() {
        return MINIMUM_PAGE_SIZE;
    }

    public int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    public int getDataScrollerMaxPages() {
        return DATA_SCROLLER_MAX_PAGES;
    }

    public int getDataScrollerPage() {
        // rich:dataScroller expects its 'page' attribute to be 1-indexed, not 0-indexed.
        return getPageControl().getPageNumber() + 1;
    }

    public void setDataScrollerPage(int i) {
        // rich:dataScroller expects its 'page' attribute to be 1-indexed, not 0-indexed.
        getPageControl().setPageNumber(i - 1);
    }

    @NotNull
    public PageControl getPageControl() {
        if (this.pageControl == null) {
            this.pageControl = loadPageControl(FacesContext.getCurrentInstance());
            if (this.pageControl == null) {
                this.pageControl = getDefaultPageControl();
                if (this.pageControl == null) {
                    throw new IllegalStateException("getDefaultPageControl() returned null - this is not allowed.");
                }
            }
        }

        /*
         * if an invalid value was already persisted to the database, this needs to be fixed;
         * this can occur when the value was valid at some point in the past, but new overrides
         * changes the valid list of page sizes; generally, the user can correct this themselves
         * at the user interface by selecting a different pageSize (which would cause the new
         * value to be persisted), unless there is only one allowable value in which case it's
         * not possible for the user to correct the issue themselves; to handle both of these
         * cases, let's just pessimistically determine whether we need to "fix" the PageControl
         * and, if so, repersist the adjusted values
         */
        if (!(this.pageControl instanceof UnlimitedPageControl)) {
            String pageSize = String.valueOf(pageControl.getPageSize());
            boolean hasValidSize = false;
            for (SelectItem validPageSize : getPageSizeSelectItems()) {
                if (validPageSize.getValue().toString().equals(pageSize)) {
                    hasValidSize = true;
                    break;
                }
            }
            if (!hasValidSize) {
                this.pageControl.setPageSize(getMinimumPageSize());
                storePageControl(FacesContext.getCurrentInstance(), this.pageControl);
            }
        }

        return this.pageControl;
    }

    public void setPageControl(PageControl pageControl) {
        // TODO: Implement equals() on PageControl, so we can skip reloading the page if the paging and sorting
        //       settings have not changed at all.
        this.pageControl = pageControl;
        storePageControl(FacesContext.getCurrentInstance(), this.pageControl);
        // Reset the data model, so it will load a new page using the new PageControl.
        this.dataModel.reset(); 
    }

    public void resetPageControl() {
        setPageControl(null);
    }

    /**
     * Subclasses can override this to change the default page size or to provide default ordering fields.
     *
     * @return the default page control (i.e. the page control to use when
     *         {@link #loadPageControl(javax.faces.context.FacesContext)} returns null); must never return null
     */
    protected PageControl getDefaultPageControl() {
        return new PageControl(0, MINIMUM_PAGE_SIZE);
    }

    /**
     * TODO
     * @param context
     * @return
     */
    protected PageControl loadPageControl(FacesContext context) {
        // TODO: Provide a default impl that stores a Map<String, PageContro> in APPLICATION scope. The key would be
        //       viewRoot+dataTableClientId.
        return null;
    }

    /**
     * Subclasses can override this to persist page controls somewhere. They would typically be keyed off the
     * current user and the current data table, so the GUI will remember a user's preferences for various tables.
     *
     * @param context
     * @param pageControl
     */
    protected void storePageControl(FacesContext context, PageControl pageControl) {
        return;
    }

    public PagedListDataModel<T> getDataModel() {
        if (this.dataModel == null) {
            this.dataModel = createDataModel();
        }
        return this.dataModel;
    }

    public void setDataModel(PagedListDataModel<T> dataModel) {
        this.dataModel = dataModel;
    }

    public abstract PagedListDataModel<T> createDataModel();
}

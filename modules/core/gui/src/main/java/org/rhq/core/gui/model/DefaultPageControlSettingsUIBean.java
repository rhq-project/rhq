package org.rhq.core.gui.model;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import javax.faces.model.SelectItem;

/**
 * @author Ian Springer
 */
@Name("DefaultPageControlSettingsUIBean")
@Scope(ScopeType.APPLICATION)
public class DefaultPageControlSettingsUIBean {
    private static final int[] PAGE_SIZES = new int[] {15, 30, 45};
    private static final SelectItem[] PAGE_SIZE_SELECT_ITEMS = new SelectItem[PAGE_SIZES.length];
    static {
        for (int i = 0; i < PAGE_SIZES.length; i++) {
            PAGE_SIZE_SELECT_ITEMS[i] = new SelectItem(PAGE_SIZES[i], Integer.valueOf(PAGE_SIZES[i]).toString());
        }
    }
    private static final int MINIMUM_PAGE_SIZE = PAGE_SIZES[0];
    private static final int DEFAULT_PAGE_SIZE = PAGE_SIZES[0];
    
    /** Limit to 7 pages until e find a general fix for RHQ-1813. */
    private static final int MAXIMUM_PAGES = 7;    

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

    public int getMaximumPages() {
        return MAXIMUM_PAGES;
    }
}

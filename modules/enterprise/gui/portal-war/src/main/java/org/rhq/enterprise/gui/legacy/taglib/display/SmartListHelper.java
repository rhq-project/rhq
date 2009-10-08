/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.taglib.display;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

/**
 * This is a little utility class that the SmartListTag uses to chop up a List of objects into small bite size pieces
 * that are more suitable for display.
 */

class SmartListHelper extends Object {
    private List masterList;
    private int pageSize;
    private int pageCount;
    private int currentPage;
    private int extSize = -1;
    private Properties prop = null;

    /**
     * Creates a SmarListHelper instance that will help you chop up a list into bite size pieces that are suitable for
     * display.
     */
    protected SmartListHelper(List list, int pageSize, Properties prop, Integer extSize) {
        super();

        if (list == null) {
            throw new IllegalArgumentException("Bad list argument passed into " + "SmartListHelper() constructor");
        } else if (pageSize < 1) {
            throw new IllegalArgumentException("Bad pageSize argument passed into " + "SmartListHelper() constructor");
        }

        this.prop = prop;
        this.pageSize = pageSize;
        this.masterList = list;

        try {
            this.extSize = extSize.intValue();
        } catch (NullPointerException e) {
            //do nothing means value  not set.
        }

        this.pageCount = this.computedPageCount();
        this.currentPage = 1;
    }

    /**
     * Returns the computed number of pages it would take to show all the elements in the list given the pageSize we are
     * working with.
     */
    protected int computedPageCount() {
        int result = 0;

        if ((this.masterList != null) && (this.pageSize > 0)) {
            int size;

            size = this.masterList.size();

            int div = size / this.pageSize;
            int mod = size % this.pageSize;
            result = (mod == 0) ? div : (div + 1);
        }

        return result;
    }

    /**
     * Returns the index into the master list of the first object that should appear on the current page that the user
     * is viewing.
     */

    protected int getFirstIndexForCurrentPage() {
        return this.getFirstIndexForPage(this.currentPage);
    }

    /**
     * Returns the index into the master list of the last object that should appear on the current page that the user is
     * viewing.
     */

    protected int getLastIndexForCurrentPage() {
        return this.getLastIndexForPage(this.currentPage);
    }

    /**
     * Returns the index into the master list of the first object that should appear on the given page.
     */

    protected int getFirstIndexForPage(int page) {
        return ((page - 1) * this.pageSize);
    }

    /**
     * Returns the index into the master list of the last object that should appear on the given page.
     */

    protected int getLastIndexForPage(int page) {
        int firstIndex = this.getFirstIndexForPage(page);
        int pageIndex = this.pageSize - 1;
        int lastIndex = this.masterList.size() - 1;

        return Math.min(firstIndex + pageIndex, lastIndex);
    }

    /**
     * Returns a subsection of the list that contains just the elements that are supposed to be shown on the current
     * page the user is viewing.
     */

    protected List getListForCurrentPage() {
        return this.getListForPage(this.currentPage);
    }

    /**
     * Returns a subsection of the list that contains just the elements that are supposed to be shown on the given page.
     */

    protected List getListForPage(int page) {
        List list = new ArrayList(this.pageSize + 1);

        int firstIndex = this.getFirstIndexForPage(page);
        int lastIndex = this.getLastIndexForPage(page);

        for (int i = firstIndex; i <= lastIndex; i++) {
            list.add(this.masterList.get(i));
        }

        return list;
    }

    /**
     * Set's the page number that the user is viewing.
     *
     * @throws IllegalArgumentException if the page provided is invalid.
     */

    protected void setCurrentPage(int page) {
        if ((page < 1) || (page > this.pageCount)) {
            Object[] objs = { new Integer(page), new Integer(pageCount) };
            throw new IllegalArgumentException(MessageFormat.format(prop.getProperty("error.msg.invalid_page"), objs));
        }

        this.currentPage = page;
    }

    /**
     * Return the little summary message that lets the user know how many objects are in the list they are viewing, and
     * where in the list they are currently positioned. The message looks like: nnn <item(s)> found, displaying nnn to
     * nnn. <item(s)> is replaced by either itemName or itemNames depending on if it should be signular or plurel.
     */

    protected String getSearchResultsSummary() {
        if (this.masterList.size() == 0) {
            Object[] objs = { prop.getProperty("paging.banner.items_name") };
            return MessageFormat.format(prop.getProperty("paging.banner.no_items_found"), objs);
        } else if (this.masterList.size() == 1) {
            Object[] objs = { prop.getProperty("paging.banner.item_name") };
            return MessageFormat.format(prop.getProperty("paging.banner.one_item_found"), objs);
        } else if (this.getFirstIndexForCurrentPage() == this.getLastIndexForCurrentPage()) {
            Object[] objs = { new Integer(this.masterList.size()), prop.getProperty("paging.banner.items_name"),
                prop.getProperty("paging.banner.items_name") };

            return MessageFormat.format(prop.getProperty("paging.banner.all_items_found"), objs);
        } else {
            Object[] objs = { new Integer(this.masterList.size()), prop.getProperty("paging.banner.items_name"),
                new Integer(this.getFirstIndexForCurrentPage() + 1), new Integer(this.getLastIndexForCurrentPage() + 1) };

            return MessageFormat.format(prop.getProperty("paging.banner.some_items_found"), objs);
        }
    }

    /**
     * Returns a string containing the nagivation bar that allows the user to move between pages within the list. The
     * urlFormatString should be a URL that looks like the following: http://.../somepage.page?page={0}
     */

    protected String getPageNavigationBar(String urlFormatString, HttpServletRequest request, boolean pageAtServer) {
        MessageFormat form = new MessageFormat(urlFormatString);

        int maxPages = 8;

        try {
            maxPages = Integer.parseInt(prop.getProperty("paging.banner.group_size"));
        } catch (NumberFormatException e) {
            // Don't care, we will just default to 8.
        }

        int currentPage = this.currentPage;
        int pageCount = this.pageCount;
        int startPage = 1;
        int endPage = maxPages;

        if ((pageCount == 1) || (pageCount == 0)) {
            return "<b>1</b>";
        }

        if (currentPage < maxPages) {
            startPage = 1;
            endPage = maxPages;
            if (pageCount < endPage) {
                endPage = pageCount;
            }
        } else {
            startPage = currentPage;
            while ((startPage + maxPages) > (pageCount + 1)) {
                startPage--;
            }

            endPage = startPage + (maxPages - 1);
        }

        boolean includeFirstLast = prop.getProperty("paging.banner.include_first_last").equals("true");
        boolean includePreviousNext = prop.getProperty("paging.banner.include_previous_next").equals("true");

        String msg = "";
        if (currentPage == 1) {
            if (includeFirstLast) {
                msg += "[" + prop.getProperty("paging.banner.first_label");
            }

            if (includePreviousNext) {
                msg += "[" + prop.getProperty("paging.banner.prev_label") + "] ";
            }
        } else {
            Object[] objs = { new Integer(currentPage - 1) };
            Object[] v1 = { new Integer(1) };
            if (includeFirstLast) {
                msg += "[<a href=\"" + form.format(v1) + "\">" + prop.getProperty("paging.banner.first_label")
                    + "</a>/<a href=\"" + form.format(objs) + "\">";
            }

            if (includePreviousNext) {
                msg += "[<a href=\"" + form.format(objs) + "\">" + prop.getProperty("paging.banner.prev_label")
                    + "</a>] ";
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            if (i == currentPage) {
                msg += "<b>" + i + "</b>";
            } else {
                Object[] v = { new Integer(i) };

                //get order and sort from request
                String sort = request.getParameter("sort");
                String order = request.getParameter("order");
                String pageSize = request.getParameter("pageSize");

                String pagMsg;

                //passing sorting, ordering, and pageSize along with every requests.
                if (order == null) {
                    pagMsg = "&order=asc";
                } else {
                    pagMsg = "&order=" + order;
                }

                if (sort == null) {
                    pagMsg = pagMsg + "&sort=1";
                } else {
                    pagMsg = pagMsg + "&sort=" + sort;
                }

                if (pageSize == null) {
                    pagMsg = pagMsg + "&pageSize=" + prop.getProperty("paging.banner.default.page_size");
                } else {
                    pagMsg = pagMsg + "&pageSize=" + pageSize;
                }

                msg += "<a href=\"" + form.format(v) + pagMsg + "\">" + i + "</a>";
            }

            if (i != endPage) {
                msg += ", ";
            } else {
                msg += " ";
            }
        }

        if (currentPage == pageCount) {
            if (includeFirstLast) {
                msg += "[" + prop.getProperty("paging.banner.next_label");
            }

            if (includePreviousNext) {
                msg += "[" + prop.getProperty("paging.banner.next_label") + "] ";
            }
        } else {
            Object[] objs = { new Integer(currentPage + 1) };
            Object[] v1 = { new Integer(pageCount) };
            if (includeFirstLast) {
                msg += "[<a href=\"" + form.format(objs) + "\">";
            }

            if (includePreviousNext) {
                msg += "[<a href=\"" + form.format(objs) + "\">" + prop.getProperty("paging.banner.next_label")
                    + "</a>] ";
            }
        }

        return msg;
    }
}
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
package org.rhq.enterprise.gui.legacy.taglib;

import java.text.MessageFormat;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * Generate pagination access for a provided PageList
 */
public class Pagination extends TagSupport {
    private static final Log log = LogFactory.getLog(Pagination.class);

    private String path;

    private JspWriter out;
    private PageList pageList;

    private String action;

    private boolean includeFirstLast = false;

    private boolean includePreviousNext = true;

    private String postfix = "";

    private int currentPage;
    private int totalPages;
    private int pageSize;

    @Override
    public void release() {
        super.release();
        out = null;
        path = null;
        pageList = null;
        this.action = null;
    }

    @Override
    public final int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        JspWriter out = pageContext.getOut();

        if (pageList.size() == pageList.getTotalSize()) {
            return SKIP_BODY;
        }

        path = request.getContextPath();

        if (this.pageList.getPageControl() == null) {
            log.error("PageLists must have valid pageControls");
        }

        this.currentPage = this.pageList.getPageControl().getPageNumber() + 1;
        if (this.pageSize == PageControl.SIZE_UNLIMITED) {
            this.totalPages = 1;
        } else {
            this.totalPages = (int) Math.ceil((double) this.pageList.getTotalSize()
                / this.pageList.getPageControl().getPageSize());
        }

        this.pageSize = this.pageList.getPageControl().getPageSize();

        try {
            out.write(createPagination());
        } catch (Exception e) {
            throw new JspException("could not generate output ", e);
        }

        return SKIP_BODY;
    }

    protected String createPagination() throws Exception {
        StringBuffer output = new StringBuffer();
        int sets = determineSets();

        output.append("<table border=\"0\" class=\"ToolbarContent\" >");
        output.append("<tr>");

        if (sets > 1) {
            output.append("<td align=\"right\" nowrap><b>");
            output.append(RequestUtils.message(pageContext, null, null, "ListToolbar.ListSetLabel", null));
            output.append("</b></td>");

            //generate select box
            output.append("<td>");
            output.append(createSetListSelect(sets));
            output.append("</select>");
            output.append("</td>");
            output.append("<td><img src=");
            output.append(path);
            output.append(" \"/images/spacer.gif\" height=\"1\" width=\"10\" border=\"0\"></td>");
        }

        output.append("<td>");
        output.append(createDots(sets));
        output.append("</td>");
        output.append("</tr>");
        output.append("</table>");

        return output.toString();
    }

    /**
     * Returns a string containing the nagivation bar that allows the user to move between pages within the list.
     *
     * <p/>The urlFormatString should be a URL that looks like the following:
     *
     * <p/>http://.../somepage.page?pn={0}
     */
    protected String createSetListSelect(int sets) {
        //ok now we should generate the select box
        StringBuffer msg = new StringBuffer();

        //generate the set list message
        msg.append("<select name=\"").append(Constants.PAGENUM_PARAM).append(postfix).append(
            "\" size=\"1\" onchange=\"goToSelectLocation(this, '").append(Constants.PAGENUM_PARAM).append(postfix)
            .append("',  '").append(getAction()).append("');\">");

        //generate the the select box with each of the lists individually.
        for (int i = 0; i < sets; i++) {
            int set = (i * Constants.MAX_PAGES);
            msg.append("<option value=\"").append(set).append("\" ");

            if (currentPage >= set) {
                msg.append(" selected=\"selected\" ");
            }

            int display = i + 1;
            msg.append(">").append(display).append("</option>");
        }

        return msg.toString();
    }

    /**
     * Returns a string containing the nagivation bar that allows the user to move between pages within the list.
     *
     * <p/>The urlFormatString should be a URL that looks like the following:
     *
     * <p/>http://.../somepage.page?pn={0}
     */
    protected String createDots(int sets) {
        setAction(removeExistingPaginationParams(getAction(), this.postfix));

        // flag to determine if we should use a ? or a &
        int index = getAction().indexOf('?');
        String separator = (index == -1) ? "?" : "&";
        MessageFormat form = new MessageFormat(getAction() + separator + "pn" + postfix + "={0}");

        int currentPage = this.pageList.getPageControl().getPageNumber();
        int pageCount = determinePageCount();

        int startPage = 0;
        int endPage = Constants.MAX_PAGES.intValue();

        int currentSet = currentPage / Constants.MAX_PAGES.intValue();
        if (sets >= 1) {
            startPage = currentSet * Constants.MAX_PAGES.intValue();
            endPage = startPage + Constants.MAX_PAGES.intValue();

            if (endPage > pageCount) {
                endPage = pageCount;
            }
        }

        if ((pageCount == 1) || (pageCount == 0)) {
            return "&nbsp;";
        }

        if (currentPage < Constants.MAX_PAGES.intValue()) {
            if (pageCount < endPage) {
                endPage = pageCount;
            }
        }

        StringBuffer msg = new StringBuffer();

        //passing sorting, ordering, and pageSize along with every request.
        StringBuffer pagMsg = new StringBuffer();
        pagMsg.append("&").append(Constants.SORTCOL_PARAM + postfix).append("=").append(
            this.pageList.getPageControl().getPrimarySortColumn()).append("&").append(
            Constants.SORTORDER_PARAM + postfix).append("=").append(
            this.pageList.getPageControl().getPrimarySortOrder()).append("&")
            .append(Constants.PAGESIZE_PARAM + postfix).append("=").append(pageSize);

        if (currentPage == startPage) {
            msg.append("<td><img src=\"").append(path).append(
                "/images/tbb_pageleft_gray.gif\" width=\"13\" height=\"16\" border=\"0\"/></td>");
        } else {
            Object[] objs = { new Integer(currentPage - 1) };
            Object[] v1 = { new Integer(1) };
            msg.append("<td><a href=\"").append(form.format(objs)).append(pagMsg.toString()).append("\">").append(
                "<img src=\"").append(path).append(
                "/images/tbb_pageleft.gif\" width=\"13\" height=\"16\" border=\"0\"/></a></td>");
        }

        int displayNumber = startPage;
        for (int i = startPage; i < endPage; i++) {
            displayNumber += 1;
            if (i == currentPage) {
                msg.append("<td>").append(displayNumber).append("</td>");
            } else {
                Object[] v = { new Integer(i) };
                msg.append("<td><a href=\"").append(form.format(v)).append(pagMsg.toString()).append("\">").append(
                    displayNumber).append("</a></td>");
            }
        }

        if (currentPage == (endPage - 1)) {
            msg.append("<td><img src=\"").append(path).append(
                "/images/tbb_pageright_gray.gif\" width=\"13\" height=\"16\" border=\"0\"/></td>");
        } else {
            Object[] objs = { new Integer(currentPage + 1) };
            Object[] v1 = { new Integer(pageCount) };
            msg.append("<td><a href=\"").append(form.format(objs)).append(pagMsg.toString()).append("\"><img src=\"")
                .append(path).append("/images/tbb_pageright.gif\" width=\"13\" height=\"16\" border=\"0\"/></a></td>");
        }

        return msg.toString();
    }

    /*
     * Removes any parameters from the query string which are going to be set again by this class.
     */
    public static String removeExistingPaginationParams(String originalActionString, String postfix) {
        int index = originalActionString.indexOf('?');
        if (index == -1) // there are no parameters so are work here is done
        {
            return originalActionString;
        }

        String mainActionString = originalActionString.substring(0, index + 1);

        String queryParametersString = originalActionString.substring(index + 1);

        log.debug("Original query parameters");
        alphaPrintParams(queryParametersString);

        String[] queryParametersArray = queryParametersString.split("&");

        // drop old parameters which we are about to appended to the query string
        StringBuilder newParameterString = new StringBuilder();
        for (int i = 0; i < queryParametersArray.length; i++) {
            String currentParam = queryParametersArray[i];
            if (!currentParam.startsWith(Constants.SORTCOL_PARAM + postfix) // sc
                && !currentParam.startsWith(Constants.SORTORDER_PARAM + postfix) // so
                && !currentParam.startsWith(Constants.PAGESIZE_PARAM + postfix) // ps
                && !currentParam.startsWith(Constants.PAGENUM_PARAM + postfix)) // pn
            {
                if (newParameterString.length() != 0) {
                    newParameterString.append("&");
                }

                newParameterString.append(currentParam);
            }
        }

        String newActionString = mainActionString + newParameterString.toString();
        log.debug("New query parameters");
        alphaPrintParams(newParameterString.toString());
        return newActionString;
    }

    private static void alphaPrintParams(String value) {
        String[] parameters = value.split("&");
        Arrays.sort(parameters);
        for (String param : parameters) {
            log.debug("\t" + param);
        }

        log.debug("\n");
    }

    private int determineSets() {
        int pageCount = determinePageCount();
        int sets;

        int div = pageCount / Constants.MAX_PAGES.intValue();
        int mod = pageCount % Constants.MAX_PAGES.intValue();

        sets = (mod == 0) ? div : (div + 1);
        return sets;
    }

    private int getPageCount() {
        return (int) Math.ceil(((double) this.pageList.getTotalSize()) / this.pageList.getPageControl().getPageSize());
    }

    private int determinePageCount() {
        int pageCount = 0;
        int size;

        size = this.pageList.getTotalSize();
        if (size == 0) {
            return 1;
        }

        int div = size / pageSize;
        int mod = size % pageSize;
        pageCount = (mod == 0) ? div : (div + 1);

        return pageCount;
    }

    @Override
    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;
    }

    public PageList getPageList() {
        return pageList;
    }

    public void setPageList(PageList pageList) {
        this.pageList = pageList;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isIncludeFirstLast() {
        return this.includeFirstLast;
    }

    public void setIncludeFirstLast(boolean includeFirstLast) {
        this.includeFirstLast = includeFirstLast;
    }

    public boolean isIncludePreviousNext() {
        return this.includePreviousNext;
    }

    public void setIncludePreviousNext(boolean includePreviousNext) {
        this.includePreviousNext = includePreviousNext;
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }
}
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.util.TaglibUtils;

/**
 * This tag takes a list of objects and creates a table to display those objects. With the help of column tags, you
 * simply provide the name of properties (get Methods) that are called against the objects in your list that gets
 * displayed [[reword that...]]
 *
 * <p/>This tag works very much like the struts iterator tag, most of the attributes have the same name and
 * functionality as the struts tag.
 *
 * <p/>Simple Usage:
 *
 * <p>
 * <p/><display:table name="list" > <display:column property="title" /> <display:column property="code" />
 * <display:column property="dean" /> </display:table>
 *
 * <p/>More Complete Usage:
 *
 * <p>
 * <p/><display:table name="list" pagesize="100"> <display:column property="title" title="College Title" width="60%"
 * sort="true" href="/display/pubs/college/edit.page" paramId="OID" paramProperty="OID" /> <display:column
 * property="code" width="10%" sort="true"/> <display:column property="primaryOfficer.name" title="Dean" width="30%" />
 * <display:column property="active" sort="true" /> </display:table>
 *
 * <p/>
 * <p/>Attributes:
 *
 * <p>
 * <p/>name property scope length offset pageSize decorator
 *
 * <p/>
 * <p/>HTML Pass-through Attributes
 *
 * <p/>There are a number of additional attributes that just get passed through to the underlying HTML table
 * declaration. With the exception of the following few default values, if these attributes are not provided, they will
 * not be displayed as part of the
 *
 * <table ...>tag.
 *
 *   <p/>width - defaults to "100%" if not provided border - defaults to "0" if not provided cellspacing - defaults to
 *   "0" if not provided cellpadding - defaults to "2" if not provided align nowrapHeader background bgcolor frame
 *   height hspace rules summary vspace
 */
public class TableTag extends TablePropertyTag {
    private List<ColumnTag> columns = new ArrayList<ColumnTag>();

    private int currentNumCols = 0;

    private int numCols = 0;

    private Decorator dec = null;

    private static Properties prop = null;

    private int pageNumber = DefaultConstants.PAGENUM_DEFAULT;

    private int pageSize = DefaultConstants.PAGESIZE_DEFAULT;

    private int offSet;

    private PageList masterList;

    /**
     * This is appended to the parameter names for sort and paging attributes
     */
    private String postfix = "";

    private List viewableList;

    private Iterator iterator;

    private HttpServletResponse res;

    private JspWriter out;

    private HttpServletRequest req;

    private StringBuilder buf = new StringBuilder(8192);

    // variables to hold the previous row columns values.
    protected Hashtable previousRow = new Hashtable(10);

    protected Hashtable nextRow = new Hashtable(10);

    private ColumnDecorator[] colDecorators;

    /**
     * Current value representing a row of data
     */
    private Object row;

    /**
     * If set only include rows where this property is set to true
     */
    private String onlyForProperty;

    private boolean started = false;

    int rowcnt = 0;

    String tableUid = Long.toString(System.currentTimeMillis());

    /**
     * static footer added using the footer tag.
     */
    private String footer;

    private final Log log = LogFactory.getLog(TableTag.class.getName());

    //------------------------------------------------------ support methods
    @Override
    public void release() {
        log.debug("releasing values");

        super.release();
        columns = new ArrayList<ColumnTag>(10);
        currentNumCols = 0;
        numCols = 0;

        pageNumber = DefaultConstants.PAGENUM_DEFAULT;
        pageSize = DefaultConstants.PAGESIZE_DEFAULT;

        buf = new StringBuilder(8192);

        // variables to hold the previous row columns values.
        previousRow = new Hashtable(10);
        nextRow = new Hashtable(10);
        colDecorators = null;

        started = false;
        rowcnt = 0;

        tableUid = Long.toString(System.currentTimeMillis());

        this.footer = null;
        this.postfix = "";
    }

    // ---------------------------------------- Communication with interior tags

    /**
     * Called by interior column tags to help this tag figure out how it is supposed to display the information in the
     *   List it is supposed to display
     *
     * @param obj an internal tag describing a column in this tableview
     */
    public void addColumn(ColumnTag obj) {
        columns.add(obj);
        if (!started) {
            currentNumCols++;
        }
    }

    private void resetColumns() {
        columns = new ArrayList<ColumnTag>(10);
        currentNumCols = 0;
    }

    // --------------------------------------------------------- Tag API methods

    /**
     * When the tag starts, we just initialize some of our variables, and do a little bit of error checking to make sure
     * that the user is not trying to give us parameters that we don't expect.
     *
     * @return value returned by super.doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
        log.debug("beginning of doStartTag()");

        prop = (Properties) pageContext.getServletContext().getAttribute(AttrConstants.PROPS_TAGLIB_NAME);
        req = (HttpServletRequest) this.pageContext.getRequest();
        res = (HttpServletResponse) this.pageContext.getResponse();
        out = pageContext.getOut();

        evaluateAttributes();

        // Load our table decorator if it is requested
        this.dec = this.loadDecorator();
        if (this.dec != null) {
            log.debug("table decorator found: " + this.dec);
            this.dec.init(this.pageContext, viewableList);
        }

        return super.doStartTag();
    }

    /**
     * Make the next collection element available and loop, or finish the iterations if there are no more elements.
     *
     * @throws JspException if a JSP exception has occurred
     */
    @Override
    public int doAfterBody() throws JspException {
        if (!started) {
            // First pass, build the headers.

            numCols = currentNumCols;

            // build an array of column decorator objects - 1 for each column tag
            colDecorators = new ColumnDecorator[currentNumCols];
            for (int c = 0; c < currentNumCols; c++) {
                ColumnTag tmpTag = columns.get(c);
                ColumnDecorator coldec = tmpTag.getDecorator();
                colDecorators[c] = coldec;
                if (colDecorators[c] != null) {
                    log.debug("adding column decorator: " + colDecorators[c]);
                    colDecorators[c].init(this.pageContext, masterList);
                }
            }

            viewableList = getViewableData();
            buf.append(this.getTableHeader());
            iterator = viewableList.iterator();
            started = true;

            if (iterator.hasNext()) {
                // Get data for first row
                this.row = iterator.next();
                String tmpvar = getVar();
                if (tmpvar != null) {
                    /* put this in a var in the page scope so that the user can have access to it
                     * in jstl expression language.
                     */
                    pageContext.setAttribute(tmpvar, this.row);
                    TaglibUtils.setScopedVariable(pageContext, "request", tmpvar, this.row);
                }

                rowcnt++;
                resetColumns();
                return (EVAL_BODY_AGAIN);
            }
        } else {
            // We're at the end of a row... generate it and see if there are more

            buf.append(generateRow(this.row, rowcnt));
            rowcnt++;
            resetColumns();

            if (iterator.hasNext()) {
                // Passes for rows
                this.row = iterator.next();
                String tmpvar = getVar();

                if (tmpvar != null) {
                    /* put this in a var in the page scope so that the user can have access to it
                     * in jstl expression language.
                     */
                    pageContext.setAttribute(tmpvar, row);
                    TaglibUtils.setScopedVariable(pageContext, "request", tmpvar, row);
                }

                resetColumns();
                return (EVAL_BODY_AGAIN);
            }
        }

        // End of data
        resetColumns();
        return (SKIP_BODY);
    }

    /**
     * Draw the table. This is where everything happens, we figure out what values we are supposed to be showing, we
     * figure out how we are supposed to be showing them, then we draw them.
     */
    @Override
    public int doEndTag() throws JspException {
        buf.append(this.getTableFooter());
        buf.append("</table>\n");

        write(buf);

        // a little clean up.
        started = false;
        buf = new StringBuilder(8192);
        rowcnt = 0;
        release();

        return EVAL_PAGE;
    }

    /**
     * This returns a list of all of the data that will be displayed on the page via the table tag. This might include
     * just a subset of the total data in the list due to to paging being active, or the user asking us to just show a
     * subset, etc...
     *
     * <p>
     * <p/>The list that is returned from here is not the original list, but it does contain references to the same
     * objects in the original list, so that means that we can sort and reorder the list, but we can't mess with the
     * data objects in the list.
     */
    public List getViewableData() throws JspException {
        //display  the entinre list if thats what the user wants
        if (pageSize == Constants.PAGESIZE_ALL) {
            return masterList;
        }

        //just return the list
        return masterList; // TODO: Shouldn't this return a subset? (ips, 04/11/07)
    }

    /**
     * Format the row as HTML.
     *
     * @param  row The list object to format as HTML.
     *
     * @return The object formatted as HTML.
     */
    protected StringBuffer generateRow(Object row, int rowcnt) throws JspException {
        log.trace("generating row with count: " + rowcnt);

        /*
         * Check if the onlyForProperty of the table tag was filled. If so, check if the property given exists and if it
         * is empty of false just return an empty StringBuffer. Otherwise continue processing. This is to selectively
         * hide rows that are contained in the List given, but which should not shown anyway (e.g. because the list is
         * generated for different targets, that have different requirements).
         *
         * TODO el-ify ..
         */
        if (onlyForProperty != null) {
            try {
                Object o = PropertyUtils.getProperty(row, onlyForProperty);
                if (o instanceof Boolean) {
                    Boolean shouldShow = (Boolean) o;
                    if ((shouldShow == null) || (shouldShow == false)) {
                        return new StringBuffer();
                    }
                } else {
                    throw new JspException("Property " + onlyForProperty + " is not boolean");
                }
            } catch (Exception e) {
                // well, we did not find the property. So complain
                throw new JspException("Error when accssing property " + onlyForProperty + ": " + e.getMessage());
            }
        }

        StringBuffer buf = new StringBuffer(8192);

        if (this.dec != null) {
            String rt = this.dec.initRow(row, rowcnt, rowcnt + offSet);
            if (rt != null) {
                buf.append(rt);
            }
        }

        try {
            for (int c = 0; c < numCols; c++) {
                if (colDecorators[c] != null) {
                    log.trace("initializing decorator: " + colDecorators[c]);
                    colDecorators[c].initRow(row, rowcnt, rowcnt + (pageSize * (this.pageNumber - 1)));
                }
            }
        } catch (Throwable t) {
            throw new JspException(t);
        }

        pageContext.setAttribute("smartRow", row);

        // Start building the row to be displayed...
        buf.append("<tr");

        if ((rowcnt % 2) == 0) {
            buf.append(" class=\"tableRowOdd\"");
        } else {
            buf.append(" class=\"tableRowEven\"");
        }

        buf.append(">\n");

        if (isLeftSidebar()) {
            buf.append("<td class=\"ListCellLine\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        // Create ID for hidden fields
        String showHideDivId = tableUid + rowcnt;
        this.pageContext.setAttribute("showHideDivId", showHideDivId);

        // Bounce through our columns and pull out the data from this object
        // that we are currently focused on (lives in "smartRow").

        for (int i = 0; i < numCols; i++) {
            ColumnTag tag = this.columns.get(i);
            ColumnDecorator colDecorator = colDecorators[i];

            // Get the hidden value to be displayed as part of this column
            Object showHideValue = null;
            if (tag.getShowHideProperty() != null) {
                String showHideProperty = tag.getShowHideProperty();

                if ((showHideProperty != null) && !showHideProperty.equals("null")) {
                    showHideValue = this.lookup(pageContext, "smartRow", showHideProperty, null, true);
                }
            }

            // Special handling for the show/hide column
            if (tag.getIsShowHideColumn() && (showHideValue != null) && !showHideValue.equals("")) {
                buf.append("<td ");
                buf.append(tag.getCellAttributes());
                buf.append(">");
                buf.append("<div onClick=\"expandcontent(this, '");
                buf.append(showHideDivId);
                buf.append("')\" class=\"showHideSwitch\"><span class=\"showstate\"></span></div></td>");
                continue;
            }

            buf.append("<td ");
            buf.append(tag.getCellAttributes());
            buf.append(">");

            // Get the value to be displayed for the column
            Object value;
            if (tag.getValue() != null) {
                try {
                    value = tag.getValue();
                    value = applyDecorator(colDecorator, value);
                } catch (NullAttributeException ne) {
                    throw new JspException("bean " + tag.getValue() + " not found");
                }
            } else {
                if (tag.getProperty() == null) {
                    // Neither property nor value set... user wants the entire row
                    value = pageContext.getAttribute(getVar());
                    value = applyDecorator(colDecorator, value);
                } else if ("ff".equals(tag.getProperty())) {
                    value = String.valueOf(rowcnt);
                } else if (tag.getProperty().equals("null")) {
                    value = ""; /* user doesn't want output, using c:set or something */
                } else {
                    value = this.lookup(pageContext, "smartRow", tag.getProperty(), null, true);
                    value = applyDecorator(colDecorator, value);
                }
            }

            // By default, we show null values as empty strings, unless the
            // user tells us otherwise.
            if ((value == null) || "".equals(value.toString().trim())) {
                if ((tag.getNulls() == null) && (prop.getProperty("basic.htmlNullValue") != null)) {
                    value = prop.getProperty("basic.htmlNullValue");
                } else {
                    value = tag.getNulls();
                }
            }

            // String to hold what's left over after value is chopped
            String leftover = "";
            boolean chopped = false;
            String tempValue = "";
            if (value != null) {
                tempValue = value.toString();
            }

            // trim the string if a maxLength or maxWords is defined
            if ((tag.getMaxLength() > 0) && (tempValue.length() > tag.getMaxLength())) {
                leftover = "..." + tempValue.substring(tag.getMaxLength(), tempValue.length());
                value = tempValue.substring(0, tag.getMaxLength()) + "...";
                chopped = true;
            } else if (tag.getMaxWords() > 0) {
                StringBuffer tmpBuffer = new StringBuffer();
                StringTokenizer st = new StringTokenizer(tempValue);
                int numTokens = st.countTokens();
                if (numTokens > tag.getMaxWords()) {
                    int x = 0;
                    while (st.hasMoreTokens() && (x < tag.getMaxWords())) {
                        tmpBuffer.append(st.nextToken() + " ");
                        x++;
                    }

                    leftover = "..." + tempValue.substring(tmpBuffer.length(), tempValue.length());
                    tmpBuffer.append("...");
                    value = tmpBuffer;
                    chopped = true;
                }
            }

            // set up a link to the data being displayed in this column if requested
            if ((tag.getAutolink() != null) && tag.getAutolink().equals("true")) {
                value = this.autoLink(value.toString());
            }

            // set up a link if href="" property is defined
            String href = null;
            if (tag.getHref() != null) {
                try {
                    href = (String) evalAttr("href", tag.getHref(), String.class);
                } catch (NullAttributeException ne) {
                    throw new JspException("bean " + tag.getHref() + " not found");
                }

                if (tag.getParamId() != null) {
                    String name = tag.getParamName();

                    if (name == null) {
                        name = "smartRow";
                    }

                    Object param = this.lookup(pageContext, name, tag.getParamProperty(), tag.getParamScope(), true);

                    // URL escape params
                    // PR: 7709
                    String paramId;
                    String paramVal;
                    String tmp = (param instanceof String) ? (String) param : param.toString();
                    try {
                        paramId = URLEncoder.encode(tag.getParamId(), "UTF-8");
                        paramVal = URLEncoder.encode(tmp, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new JspException(
                            "could not encode ActionForward path parameters because the JVM does not support UTF-8!?",
                            e);
                    }

                    // flag to determine if we should use a ? or a &
                    int index = href.indexOf('?');
                    String separator = (index == -1) ? "?" : "&";

                    // if value has been chopped, add leftover as title
                    if (chopped) {
                        value = "<a href=\"" + href + separator + paramId + "=" + paramVal + "\" title=\"" + leftover
                            + "\">" + value + "</a>";
                    } else {
                        value = "<a href=\"" + href + separator + paramId + "=" + paramVal + "\">" + value + "</a>";
                    }
                } else /* tag.getParamId() == null */
                {
                    // if value has been chopped, add leftover as title
                    if (chopped) {
                        value = "<a href=\"" + href + "\" title=\"" + leftover + "\">" + value + "</a>";
                    } else {
                        value = "<a href=\"" + href + "\">" + value + "</a>";
                    }
                }
            }

            if (chopped && (href == null)) {
                buf.append(value.toString().substring(0, value.toString().length() - 3));
                buf.append("<a style=\"cursor: help;\" title=\"" + leftover + "\">");
                buf.append(value.toString().substring(value.toString().length() - 3, value.toString().length())
                    + "</a>");
            } else {
                buf.append(value);
            }

            // Append the hidden text to the text in this cell
            // NOTE: This assumes the class of switchcontent, however in the future the need
            // may arise to make this variable.
            if ((showHideValue != null) && !showHideValue.equals("")) {
                showHideValue = applyDecorator(colDecorator, showHideValue);

                buf.append("<div id=\"");
                buf.append(showHideDivId);
                buf.append("\" class=\"switchcontent\">");
                buf.append(showHideValue.toString());
                buf.append("</div>");
            }

            buf.append("</td>\n");
        }

        // special case, if they didn't provide any columns.
        if (numCols == 0) {
            buf.append("<td class=\"tableCell\">");
            buf.append(row.toString());
            buf.append("</td>");
        }

        if (isRightSidebar()) {
            buf.append("<td class=\"ListCellLine\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        buf.append("</tr>\n");

        if (this.dec != null) {
            String rt = this.dec.finishRow();
            if (rt != null) {
                buf.append(rt);
            }
        }

        for (int c = 0; c < numCols; c++) {
            if (colDecorators[c] != null) {
                colDecorators[c].finishRow();
            }
        }

        if (this.dec != null) {
            this.dec.finish();
        }

        this.dec = null;

        for (int c = 0; c < numCols; c++) {
            if (colDecorators[c] != null) {
                colDecorators[c].finish();
            }
        }

        return buf;
    }

    private Object applyDecorator(ColumnDecorator colDecorator, Object value) throws JspException {
        if (colDecorator != null) {
            try {
                value = colDecorator.decorate(value);
            } catch (RuntimeException e) {
                log.warn("Exception decorating column", e);
                throw new JspException("Decorator " + colDecorator + " encountered a problem: ", e);
            } catch (Exception e) {
                throw new JspException("Decorator " + colDecorator + " encountered a problem: ", e);
            }
        }

        return value;
    }

    // -------------------------------------------------------- Utility Methods

    private String spacerImg() {
        return req.getContextPath() + "/images/spacer.gif";
    }

    private String makeUrl(boolean makeQueryStringOpenEnded, boolean removePageControlParams) throws JspException {
        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
        String url = (getAction() != null) ? getAction() : request.getRequestURI();
        Map params = RequestUtils.computeParameters(pageContext, getParamId(), getParamName(), getParamProperty(),
            getParamScope(), null, null, null, false);
        try {
            url = RequestUtils.computeURL(pageContext, null, url, null, null, params, null, false);
        } catch (Exception e) {
            throw new JspException("Couldn't compute URL:" + e);
        }

        if (removePageControlParams) {
            url = removePageControlParams(url);
        }

        if (makeQueryStringOpenEnded) {
            url = makeQueryStringOpenEnded(url);
        }

        return url;
    }

    private String removePageControlParams(String url) {
        int index = url.indexOf('?');
        if (index != -1) {
            String base = url.substring(0, index);
            StringBuilder buf = new StringBuilder(base);
            String queryString = url.substring(index + 1);
            Map<String, Object> params = HttpUtils.parseQueryString(queryString);
            params.remove(ParamConstants.SORTCOL_PARAM);
            params.remove(ParamConstants.SORTORDER_PARAM);
            params.remove(ParamConstants.PAGESIZE_PARAM);
            params.remove(ParamConstants.PAGENUM_PARAM);
            if (!params.isEmpty()) {
                buf.append('?').append(convertMapToQueryString(params));
            }

            url = buf.toString();
        }

        return url;
    }

    private String makeQueryStringOpenEnded(String url) {
        if (url.indexOf('?') == -1) {
            url += '?';
        } else if (!url.endsWith("&")) {
            url += '&';
        }

        return url;
    }

    /**
     * Generates the table header, including the first row of the table which displays the titles of the various
     * columns.
     *
     * @return Table header in HTML format
     */
    protected String getTableHeader() throws JspException {
        log.trace("generating table header");

        StringBuilder buf = new StringBuilder(1024);
        HttpServletRequest req = (HttpServletRequest) this.pageContext.getRequest();
        String url = makeUrl(true, true);

        buf.append("<table ");
        buf.append(this.getTableAttributes());
        buf.append(">\n");

        // If they don't want the header shown for some reason, then stop here.
        if ((prop.getProperty("basic.show.header") != null) && !prop.getProperty("basic.show.header").equals("true")) {
            return buf.toString();
        }

        buf.append("<tr class=\"tableRowHeader\">\n");

        if (isLeftSidebar()) {
            buf.append("<td class=\"ListCellLineEmpty\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        // if one of the columns declares a colspan, we'll set this to
        // the number of subsequent columns to skip
        int colsToSkip = 0;

        for (int i = 0; i < currentNumCols; i++) {
            LocalizedColumnTag tag = (LocalizedColumnTag) this.columns.get(i);

            if (colsToSkip > 0) {
                // we're in the middle of a colspan, so skip this
                // column
                colsToSkip--;
                continue;
            } else if (tag.getHeaderColspan() != null) {
                Integer colspan;
                try {
                    colspan = (Integer) evalAttr("headerColspan", tag.getHeaderColspan(), Integer.class);
                } catch (NullAttributeException ne) {
                    throw new JspException("bean " + tag.getHeaderColspan() + " not found");
                }

                // start the colspan
                colsToSkip = colspan;
            }

            String sortAttr = tag.getSortAttr();

            buf.append("<th");
            if (tag.getWidth() != null) {
                buf.append(" width=\"" + tag.getWidth() + "\"");
            }

            if (tag.getAlign() != null) {
                buf.append(" align=\"" + tag.getAlign() + "\"");
            }

            // if nowrapHeader
            if (getNowrapHeader() != null) {
                buf.append(" nowrap=\"true\"");
            }

            if (colsToSkip > 0) {
                buf.append(" colspan=\"" + colsToSkip + "\"");

                // decrement colsToSkip to account for the declaring
                // column
                colsToSkip--;
            }

            String header;
            try {
                header = (String) evalAttr("title", tag.getTitle(), String.class);
            } catch (NullAttributeException ne) {
                throw new JspException("bean " + tag.getTitle() + " not found");
            }

            if (header == null) {
                if (tag.getIsLocalizedTitle()) {
                    header = StringUtil.toUpperCaseAt(tag.getProperty(), 0);
                } else {
                    header = "<img src=\"" + spacerImg() + "\" width=\"1\" height=\"1\" border=\"0\"/>";
                }
            }

            buf.append(" class=\"tableRowInactive\">");

            /*
             * start multi-column sort rendering
             */
            PageControl pc = masterList.getPageControl();
            int sortIndex = 0;
            String sortimg = null;
            String newSortOrder = Constants.SORTORDER_ASC; // default
            boolean wasSortedOn = false;

            if (sortAttr != null) {
                for (OrderingField field : pc.getOrderingFields()) {
                    sortIndex++;
                    if (field.getField().equals(sortAttr)) {
                        if (field.getOrdering().equals(PageOrdering.ASC)) {
                            newSortOrder = Constants.SORTORDER_DEC;
                            if (sortIndex == 1) {
                                sortimg = "/images/tb_sortup.gif";
                            } else {
                                sortimg = "/images/tb_sortup_inactive.gif";
                            }
                        } else {
                            newSortOrder = Constants.SORTORDER_ASC;
                            if (sortIndex == 1) {
                                sortimg = "/images/tb_sortdown.gif";
                            } else {
                                sortimg = "/images/tb_sortdown_inactive.gif";
                            }
                        }

                        wasSortedOn = true;

                        break;
                    }
                }

                buf.append("<a href=\"");

                buf.append(url);
                buf.append(ParamConstants.SORTORDER_PARAM).append(this.postfix);
                buf.append("=").append(newSortOrder);

                buf.append("&");
                buf.append(ParamConstants.SORTCOL_PARAM).append(this.postfix);
                buf.append("=").append(sortAttr);

                buf.append("&");
                buf.append(ParamConstants.PAGESIZE_PARAM).append(this.postfix);
                buf.append("=").append(masterList.getPageControl().getPageSize());

                buf.append("&");
                buf.append(ParamConstants.PAGENUM_PARAM).append(this.postfix);
                buf.append("=").append(masterList.getPageControl().getPageNumber());

                buf.append("\">");

                buf.append(header);

                if (sortimg != null) {
                    buf.append("<img border=\"0\" src=\"");
                    buf.append(req.getContextPath());
                    buf.append(sortimg);
                    buf.append("\" >");
                }

                if (wasSortedOn) {
                    buf.append(" " + String.valueOf(sortIndex));
                }

                buf.append("</a>");
            } else {
                buf.append(header);
            }

            buf.append("</th>\n");
        }

        // Special case, if they don't provide any columns.
        if (currentNumCols == 0) {
            buf.append("<td><b>" + prop.getProperty("error.msg.no_column_tags") + "</b></td>");
        }

        if (isRightSidebar()) {
            buf.append("<td class=\"ListCellLineEmpty\"><img src=\"");
            buf.append(spacerImg());
            buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
        }

        buf.append("</tr>\n");

        if (this.footer != null) {
            buf.append("<tfoot>");
            buf.append(this.footer);
            buf.append("</tfoot>");

            // reset footer
            this.footer = null;
        }

        String ret = buf.toString();
        return ret;
    }

    /**
     * Generates table footer with links for export commands.
     *
     * @return HTML formatted table footer
     */
    protected String getTableFooter() throws JspException {
        log.trace("generating footer");

        StringBuilder buf = new StringBuilder(1024);

        String url = makeUrl(true, false);

        if (getExport() != null) {
            buf.append("<tr><td align=\"left\" width=\"100%\" colspan=\"").append(currentNumCols).append("\">");
            buf.append("<table width=\"100%\" border=\"0\" cellspacing=\"0\" ");
            buf.append("cellpadding=0><tr class=\"tableRowAction\">");
            buf.append("<td align=\"left\" valign=\"bottom\" class=\"");
            buf.append("tableCellAction\">");

            // Figure out what formats they want to export, make up a little string

            String formats = "";
            if ((prop.getProperty("export.csv") != null) && prop.getProperty("export.csv").equals("true")) {
                formats += "<a href=\"" + url + "exportType=1\">" + prop.getProperty("export.csv.label") + "</a>\n";
            }

            if ((prop.getProperty("export.excel") != null) && prop.getProperty("export.excel").equals("true")) {
                if (!formats.equals("")) {
                    formats += prop.getProperty("export.banner.sepchar");
                }

                formats += "<a href=\"" + url + "exportType=2\">" + prop.getProperty("export.excel.label") + "</a>\n";
            }

            if ((prop.getProperty("export.xml") != null) && prop.getProperty("export.xml").equals("true")) {
                if (!formats.equals("")) {
                    formats += prop.getProperty("export.banner.sepchar");
                }

                formats += "<a href=\"" + url + "exportType=3\">" + prop.getProperty("export.xml.label") + "</a>\n";
            }

            Object[] objs = { formats };
            if (prop.getProperty("export.banner") != null) {
                buf.append(MessageFormat.format(prop.getProperty("export.banner"), objs));
            }

            buf.append("</td></tr>");
            buf.append("</table>\n");
            buf.append("</td></tr>");
        }

        String tmpEmptyMsg = getEmptyMsg();
        if (isPadRows()) {
            int tableSize = this.pageSize;
            if (tableSize < 1) {
                tableSize = Constants.PAGESIZE_DEFAULT;
            }

            if (tableSize > rowcnt) {
                String src = spacerImg();
                for (int i = rowcnt; i < tableSize; i++) {
                    buf.append("<tr class=\"ListRow\">\n");
                    if (isLeftSidebar()) {
                        buf.append("<td class=\"ListCellLine\"><img src=\"");
                        buf.append(src);
                        buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
                    }

                    for (int j = 0; j < numCols; j++) {
                        buf.append("<td class=\"ListCell\">&nbsp;</td>\n");
                    }

                    if (isRightSidebar()) {
                        buf.append("<td class=\"ListCellLine\"><img src=\"");
                        buf.append(src);
                        buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
                    }

                    buf.append("</tr>\n");
                }
            }
        } else if ((tmpEmptyMsg != null) && (rowcnt == 0)) {
            // there is a message to display when there are no rows
            String src = spacerImg();
            buf.append("<tr class=\"ListRow\">\n");
            if (isLeftSidebar()) {
                buf.append("<td class=\"ListCellLine\"><img src=\"");
                buf.append(src);
                buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
            }

            for (int j = 0; j < numCols; j++) {
                buf.append("<td class=\"ListCell\"");
                if (j == 1) {
                    buf.append(" nowrap=\"true\"><i>").append(tmpEmptyMsg).append("</i>");
                } else {
                    buf.append(">&nbsp;");
                }

                buf.append("</td>\n");
            }

            if (isRightSidebar()) {
                buf.append("<td class=\"ListCellLine\"><img src=\"");
                buf.append(src);
                buf.append("\" width=\"5\" height=\"1\" border=\"0\"></td>\n");
            }

            buf.append("</tr>\n");
        }

        String tmpstr = buf.toString();
        return tmpstr;
    }

    /**
     * This takes a cloumn value and grouping index as the argument. It then groups the column and returns the
     * appropriate string back to the caller.
     */
    protected String group(String value, int group) {
        if ((group == 1) & (this.nextRow.size() > 0)) { // we are at the begining of the next row so copy the contents from .

            // nextRow to the previousRow.
            this.previousRow.clear();
            this.previousRow.putAll(nextRow);
            this.nextRow.clear();
        }

        if (!this.nextRow.containsKey(new Integer(group))) {
            // Key not found in the nextRow so adding this key now... remember all the old values.
            this.nextRow.put(group, value);
        }

        /**
         *  Start comparing the value we received, along with the grouping index.
         *  if no matching value is found in the previous row then return the value.
         *  if a matching value is found then this value should not get printed out
         *  so reuturn ""
         **/

        if (this.previousRow.containsKey(new Integer(group))) {
            for (int x = 1; x <= group; x++) {
                if (!(this.previousRow.get(new Integer(x))).equals((this.nextRow.get(new Integer(x))))) {
                    // no match found so return this value back to the caller.
                    return value;
                }
            }
        }

        /**
         * This is used, for when there is no data in the previous row,
         * It gets used only the firt time.
         **/

        if (this.previousRow.size() == 0) {
            return value;
        }

        // There is corresponding value in the previous row so this value need not be printed, return ""
        return "<!-- returning from table tag -->"; // we are done !.
    }

    /* XXX not used?
     * protected void clearHashForColumGroupings() { // clear the hashes so that the hash does not have any reside from
     * previous reports and not cause any // problems. this.previousRow.clear(); this.nextRow.clear(); }
     */

    /**
     * Takes all the table pass-through arguments and bundles them up as a string that gets tacked on to the end of the
     * table tag declaration.
     *
     * <p>
     * <p/>Note that we override some default behavior, specifically:
     *
     * <p>
     * <p/>width defaults to 100% if not provided border defaults to 0 if not provided cellspacing defaults to 1 if not
     * provided cellpadding defaults to 2 if not provided
     */
    protected String getTableAttributes() {
        StringBuffer results = new StringBuffer();

        if (getStyleClass() != null) {
            results.append(" class=\"");
            results.append(getStyleClass());
            results.append("\"");
        } else {
            results.append(" class=\"table\"");
        }

        if (getStyleId() != null) {
            results.append(" id=\"");
            results.append(getStyleId());
            results.append("\"");
        } else {
            results.append(" class=\"table\"");
        }

        if (getWidth() != null) {
            results.append(" width=\"");
            results.append(getWidth());
            results.append("\"");
        } else {
            results.append(" width=\"100%\"");
        }

        if (getBorder() != null) {
            results.append(" border=\"");
            results.append(getBorder());
            results.append("\"");
        } else {
            results.append(" border=\"0\"");
        }

        if (getCellspacing() != null) {
            results.append(" cellspacing=\"");
            results.append(getCellspacing());
            results.append("\"");
        } else {
            results.append(" cellspacing=\"1\"");
        }

        if (getCellpadding() != null) {
            results.append(" cellpadding=\"");
            results.append(getCellpadding());
            results.append("\"");
        } else {
            results.append(" cellpadding=\"2\"");
        }

        if (getAlign() != null) {
            results.append(" align=\"");
            results.append(getAlign());
            results.append("\"");
        }

        if (getBackground() != null) {
            results.append(" background=\"");
            results.append(getBackground());
            results.append("\"");
        }

        if (getBgcolor() != null) {
            results.append(" bgcolor=\"");
            results.append(getBgcolor());
            results.append("\"");
        }

        if (getFrame() != null) {
            results.append(" frame=\"");
            results.append(getFrame());
            results.append("\"");
        }

        if (getHeight() != null) {
            results.append(" height=\"");
            results.append(getHeight());
            results.append("\"");
        }

        if (getHspace() != null) {
            results.append(" hspace=\"");
            results.append(getHspace());
            results.append("\"");
        }

        if (getRules() != null) {
            results.append(" rules=\"");
            results.append(getRules());
            results.append("\"");
        }

        if (getSummary() != null) {
            results.append(" summary=\"");
            results.append(getSummary());
            results.append("\"");
        }

        if (getVspace() != null) {
            results.append(" vspace=\"");
            results.append(getVspace());
            results.append("\"");
        }

        return results.toString();
    }

    /**
     * This functionality is borrowed from struts, but I've removed some struts specific features so that this tag can
     * be used both in a struts application, and outside of one.
     *
     * <p/>Locate and return the specified bean, from an optionally specified scope, in the specified page context. If
     * no such bean is found, return <code>null</code> instead.
     *
     * @param  pageContext Page context to be searched
     * @param  name        Name of the bean to be retrieved
     * @param  scope       Scope to be searched (page, request, session, application) or <code>null</code> to use <code>
     *                     findAttribute()</code> instead
     *
     * @throws JspException if an invalid scope name is requested
     */

    public Object lookup(PageContext pageContext, String name, String scope) throws JspException {
        log.trace("looking up: " + name + " in scope: " + scope);

        Object bean;
        if (scope == null) {
            bean = pageContext.findAttribute(name);
        } else if (scope.equalsIgnoreCase("page")) {
            bean = pageContext.getAttribute(name, PageContext.PAGE_SCOPE);
        } else if (scope.equalsIgnoreCase("request")) {
            bean = pageContext.getAttribute(name, PageContext.REQUEST_SCOPE);
        } else if (scope.equalsIgnoreCase("session")) {
            bean = pageContext.getAttribute(name, PageContext.SESSION_SCOPE);
        } else if (scope.equalsIgnoreCase("application")) {
            bean = pageContext.getAttribute(name, PageContext.APPLICATION_SCOPE);
        } else {
            Object[] objs = { name, scope };
            if (prop.getProperty("error.msg.cant_find_bean") != null) {
                String msg = MessageFormat.format(prop.getProperty("error.msg.cant_find_bean"), objs);
                throw new JspException(msg);
            } else {
                throw new JspException("Could not find " + name + " in scope " + scope);
            }
        }

        return (bean);
    }

    /**
     * This functionality is borrowed from struts, but I've removed some struts specific features so that this tag can
     * be used both in a struts application, and outside of one.
     *
     * <p/>Locate and return the specified property of the specified bean, from an optionally specified scope, in the
     * specified page context.
     *
     * @param  pageContext Page context to be searched
     * @param  name        Name of the bean to be retrieved
     * @param  property    Name of the property to be retrieved, or <code>null</code> to retrieve the bean itself
     * @param  scope       Scope to be searched (page, request, session, application) or <code>null</code> to use <code>
     *                     findAttribute()</code> instead
     *
     * @throws JspException if an invalid scope name is requested
     * @throws JspException if the specified bean is not found
     * @throws JspException if accessing this property causes an IllegalAccessException, IllegalArgumentException,
     *                      InvocationTargetException, or NoSuchMethodException
     */

    public Object lookup(PageContext pageContext, String name, String property, String scope, boolean useDecorator)
        throws JspException {
        log.trace("looking up: " + name + ":" + property + " in scope: " + scope);

        if (useDecorator && (this.dec != null)) {
            // First check the decorator, and if it doesn't return a value
            // then check the inner object...
            try {
                if (property == null) {
                    return this.dec;
                }

                return (PropertyUtils.getProperty(this.dec, property));
            } catch (IllegalAccessException e) {
                log.debug("bean access failed:", e);
                Object[] objs = { name, this.dec };
                if (prop.getProperty("error.msg.illegal_access_exception") != null) {
                    throw new JspException(MessageFormat.format(prop.getProperty("error.msg.illegal_access_exception"),
                        objs));
                } else {
                    throw new JspException("IllegalAccessException trying to fetch " + "property " + name
                        + " from bean " + dec);
                }
            } catch (InvocationTargetException e) {
                log.debug("bean invocation failed:", e);
                Object[] objs = { name, this.dec };
                if (prop.getProperty("error.msg.invocation_target_exception") != null) {
                    throw new JspException(MessageFormat.format(prop
                        .getProperty("error.msg.invocation_target_exception"), objs));
                } else {
                    throw new JspException("InvocationTargetException trying to fetch " + "property " + name
                        + " from bean " + dec);
                }
            } catch (NoSuchMethodException e) {
                log.debug("bean getter property access failed:", e);
                throw new JspException(" bean property getter not found");
            }
        }

        // Look up the requested bean, and return if requested
        Object bean = this.lookup(pageContext, name, scope);
        if (property == null) {
            return (bean);
        }

        if (bean == null) {
            log.debug("expected bean was null");
            Object[] objs = { name, scope };
            if (prop.getProperty("error.msg.cant_find_bean") != null) {
                throw new JspException(MessageFormat.format(prop.getProperty("error.msg.cant_find_bean"), objs));
            } else {
                throw new JspException("Could not find bean " + name + "in scope " + scope);
            }
        }

        // Locate and return the specified property
        try {
            return (PropertyUtils.getProperty(bean, property));
        } catch (IllegalAccessException e) {
            Object[] objs = { property, name };
            log.debug("bean access failed:", e);
            if (prop.getProperty("error.msg.illegal_access_exception") != null) {
                throw new JspException(MessageFormat.format(prop.getProperty("error.msg.illegal_access_exception"),
                    objs));
            } else {
                throw new JspException("IllegalAccessException trying to fetch " + "property " + property
                    + " from bean " + name);
            }
        } catch (InvocationTargetException e) {
            Object[] objs = { property, name };
            if (prop.getProperty("error.msg.invocation_target_exception") != null) {
                throw new JspException(MessageFormat.format(prop.getProperty("error.msg.invocation_target_exception"),
                    objs));
            } else {
                throw new JspException("InvocationTargetException trying to fetch " + "property " + name
                    + " from bean " + dec);
            }
        } catch (NoSuchMethodException e) {
            log.debug("bean getter property access failed:", e);
            throw new JspException(" bean getter for property " + property + "  not found in bean " + name);
        }
    }

    /**
     * If the user has specified a decorator, then this method takes care of creating the decorator (and checking to
     * make sure it is a subclass of the TableDecorator object). If there are any problems loading the decorator then
     * this will throw a JspException which will get propogated up the page.
     */

    protected Decorator loadDecorator() throws JspException {
        log.trace("loading decorator");

        if ((getDecorator() == null) || (getDecorator().length() == 0)) {
            return null;
        }

        try {
            Class c = Class.forName(getDecorator());

            if (!Class.forName("org.rhq.enterprise.gui.legacy.taglib.display.Decorator").isAssignableFrom(c)) {
                throw new JspException("invalid decorator");
            }

            Decorator d = (Decorator) c.newInstance();
            log.debug("found decorator: " + d);
            return d;
        } catch (Exception e) {
            log.debug("loading and instantiating decorator failed: ", e);
            throw new JspException("failure loading and instanting decorator " + e.toString());
        }
    }

    /**
     * This takes the string that is passed in, and "auto-links" it, it turns email addresses into hyperlinks, and also
     * turns things that looks like URLs into hyperlinks as well. The rules are currently very basic, In Perl regex
     * lingo...
     *
     * <p/>Email: \b\S+\@[^\@\s]+\b URL: (http|https|ftp)://\S+\b
     *
     * <p/>I'm doing this via brute-force since I don't want to be dependent on a third party regex package.
     */
    protected String autoLink(String data) {
        String work = data;
        int index;
        String results = "";

        if ((data == null) || (data.length() == 0)) {
            return data;
        }

        // First check for email addresses.

        while ((index = work.indexOf("@")) != -1) {
            int start = 0;
            int end = work.length() - 1;

            // scan backwards...
            for (int i = index; i >= 0; i--) {
                if (Character.isWhitespace(work.charAt(i))) {
                    start = i + 1;
                    break;
                }
            }

            // scan forwards...
            for (int i = index; i <= end; i++) {
                if (Character.isWhitespace(work.charAt(i))) {
                    end = i - 1;
                    break;
                }
            }

            String email = work.substring(start, (end - start + 1));

            results = results + work.substring(0, start) + "<a href=\"mailto:" + email + "\">" + email + "</a>";

            if (end == work.length()) {
                work = "";
            } else {
                work = work.substring(end + 1);
            }
        }

        work = results + work;
        results = "";

        // Now check for urls...

        while ((index = work.indexOf("http://")) != -1) {
            int end = work.length() - 1;

            // scan forwards...
            for (int i = index; i <= end; i++) {
                if (Character.isWhitespace(work.charAt(i))) {
                    end = i - 1;
                    break;
                }
            }

            String url = work.substring(index, (end - index + 1));

            results = results + work.substring(0, index) + "<a href=\"" + url + "\">" + url + "</a>";

            if (end == work.length()) {
                work = "";
            } else {
                work = work.substring(end + 1);
            }
        }

        results += work;
        return results;
    }

    /**
     * Called by the setProperty tag to override some default behavior or text string.
     */
    public void setProperty(String name, String value) {
        prop.setProperty(name, value);
    }

    /**
     * Sets the content of the footer. Called by a nested footer tag.
     *
     * @param string footer content
     */
    public void setFooter(String string) {
        this.footer = string;
    }

    /**
     * Is this the first iteration?
     *
     * @return boolean <code>true</code> if this is the first iteration
     */
    protected boolean isFirstIteration() {
        if (log.isDebugEnabled()) {
            log.debug("[" + getId() + "] first iteration=" + (this.rowcnt == 0) + " (row number=" + this.rowcnt + ")");
        }

        // in first iteration rowcnt is 0
        // (rowcnt is incremented in doAfterBody)
        return this.rowcnt == 0;
    }

    /**
     * Use the jstl expression expression language to evaluate a field.
     *
     * @param  name
     * @param  value
     * @param  type  The Class type of the object you expect.
     *
     * @return The object found
     *
     * @throws NullAttributeException Thrown if the value is null.
     */
    private Object evalAttr(String name, String value, Class type) throws JspTagException {
        try {
            return ExpressionUtil.evalNotNull("display", name, value, type, this, pageContext);
        } catch (NullAttributeException ne) {
            throw new JspTagException("Attribute " + name + " not found in TableTag");
        } catch (JspException je) {
            throw new JspTagException(je.toString());
        }
    }

    protected void evaluateAttributes() throws JspTagException {
        masterList = getItems();
        offSet = (Integer) evalAttr("offset", getOffset(), Integer.class);
        pageNumber = this.masterList.getPageControl().getPageNumber();
        this.pageSize = this.masterList.getPageControl().getPageSize();
    }

    public String getPostfix() {
        return postfix;
    }

    public void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    /**
     * Converts a map containing query string parameters into an HTTP query string.
     *
     * @param  params a map containing the query string parameters; the values may be stored as either String Arrays
     *                (for multi-value parameters) or non-Array Objects (for single-value parameters); null values are
     *                allowed; non-String Array values are not allowed
     *
     * @return an HTTP query string
     */
    private static String convertMapToQueryString(Map<String, Object> params) {
        if ((params == null) || params.isEmpty()) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        for (String paramName : params.keySet()) {
            Object paramValueObj = params.get(paramName);
            if (paramValueObj instanceof String[]) {
                String[] paramValues = (String[]) paramValueObj;
                for (String paramValue : paramValues) {
                    buf.append(paramName).append('=').append(paramValue).append('&');
                }
            } else {
                buf.append(paramName).append('=');
                if ((paramValueObj != null) && !paramValueObj.getClass().isArray()) {
                    buf.append(paramValueObj);
                }

                buf.append('&');
            }
        }

        buf.deleteCharAt(buf.length() - 1);
        return buf.toString();
    }

    /**
     * @return the onlyForProperty
     */
    public String getOnlyForProperty() {
        return onlyForProperty;
    }

    /**
     * @param onlyForProperty the onlyForProperty to set
     */
    public void setOnlyForProperty(String onlyForProperty) {
        this.onlyForProperty = onlyForProperty;
    }
}
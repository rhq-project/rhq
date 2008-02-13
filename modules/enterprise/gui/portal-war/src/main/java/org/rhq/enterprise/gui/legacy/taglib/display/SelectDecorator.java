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
/*
 * Created on Apr 16, 2003
 *
 */
package org.rhq.enterprise.gui.legacy.taglib.display;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;

/**
 * This decorator writes whatever is in the value attribute
 */
public class SelectDecorator extends BaseDecorator {
    private static final String VALUE_KEY = "value";
    private static final String LABEL_KEY = "label";

    private static Log log = LogFactory.getLog(SelectDecorator.class.getName());

    private String onchange;
    private String optionList;
    private String selectId;

    private String onchange_el;
    private List optionList_el;
    private Integer selectedId_el;

    /**
     * don't skip the body
     */
    public int doStartTag() throws JspTagException {
        Object parent = getParent();

        if ((parent == null) || !(parent instanceof ColumnTag)) {
            throw new JspTagException("A BaseDecorator must be used within a ColumnTag.");
        }

        ((ColumnTag) parent).setDecorator(this);

        return SKIP_BODY;
    }

    /**
     * tag building is done in the buildTag method. This method is not implemented because the table body must be
     * evaluated first
     *
     * @see org.rhq.enterprise.gui.legacy.taglib.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object obj) {
        return buildTag();
    }

    /**
     * build the tag
     */
    private String buildTag() {
        String val = "";
        StringBuffer error = new StringBuffer();
        try {
            onchange_el = (String) evalAttr("onchange", onchange, String.class);
        } catch (NullAttributeException e) {
            error.append(generateErrorComment(e.getClass().getName(), "onchange_el", getOnchange(), e));
        } catch (JspException e) {
            error.append(generateErrorComment(e.getClass().getName(), "onchange_el", getOnchange(), e));
        }

        try {
            optionList_el = (List) evalAttr("optionList", optionList, List.class);
        } catch (NullAttributeException e) {
            error.append(generateErrorComment(e.getClass().getName(), "onchange_el", "", e));
        } catch (JspException e) {
            error.append(generateErrorComment(e.getClass().getName(), "onchange_el", "", e));
        }

        try {
            selectedId_el = (Integer) evalAttr("selected", selectId, Integer.class);
        } catch (NullAttributeException e) {
            error.append(generateErrorComment(e.getClass().getName(), "selected_el", getSelectedId().toString(), e));
        } catch (JspException e) {
            error.append(generateErrorComment(e.getClass().getName(), "selected_el", getSelectedId().toString(), e));
        }

        if (error.length() > 0) {
            return error.toString();
        }

        return generateOutput();
    }

    private String generateOutput() {
        List list = getOptionList();

        // do nothing for a null list or list size is zero
        if ((list == null) || (list.size() == 0)) {
            return "";
        }

        // for list with one item, just return the string of the label
        if (list.size() == 1) {
            Iterator lIterator = list.iterator();
            Map items = (Map) lIterator.next();
            return (String) items.get(LABEL_KEY);
        }

        StringBuffer sb = new StringBuffer("<select ");
        sb.append("onchange=\"");
        sb.append(getOnchange()).append("\">");

        Iterator lIterator = list.iterator();
        while (lIterator.hasNext()) {
            Map items = (Map) lIterator.next();
            String val = (String) items.get(VALUE_KEY);
            String label = (String) items.get(LABEL_KEY);
            Integer intVal = new Integer(val);

            sb.append("<option ");
            if (intVal.intValue() == getSelectedId().intValue()) {
                sb.append("\" selected=\"selected\" ");
            }

            sb.append(" value=\"");
            sb.append(val).append("\" >");
            sb.append(label).append("</option>");
        }

        sb.append("</select>");
        return sb.toString();
    }

    /**
     * @return
     */
    public String getOnchange() {
        return onchange_el;
    }

    /**
     * @param string
     */
    public void setOnchange(String string) {
        onchange = string;
    }

    /**
     * @return
     */
    public List getOptionList() {
        return optionList_el;
    }

    /**
     * @param string
     */
    public void setOptionItems(String string) {
        optionList = string;
    }

    /**
     * @return
     */
    public Integer getSelectedId() {
        return selectedId_el;
    }

    /**
     * @param string
     */
    public void setSelectId(String string) {
        selectId = string;
    }
}
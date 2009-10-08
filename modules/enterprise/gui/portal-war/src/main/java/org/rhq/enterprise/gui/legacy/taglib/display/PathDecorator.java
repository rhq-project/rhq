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

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.gui.legacy.util.TaglibUtils;

/**
 * Re-evaluate the value to be display by the column
 */
public class PathDecorator extends BaseDecorator {
    private static Log log = LogFactory.getLog(PathDecorator.class.getName());

    private boolean strict = false;

    private int postChars;

    private int preChars;

    private String styleClass = null;

    /**
     * If string column value exists, use that. Otherwise, return 2nd choice.
     *
     * @see org.apache.taglibs.display.ColumnDecorator#decorate(java.lang.Object)
     */
    public String decorate(Object columnValue) {
        String realPath = columnValue.toString();
        String shortPath = TaglibUtils.shortenPath(columnValue.toString(), preChars, postChars, strict);

        if (realPath.equals(shortPath)) {
            return realPath;
        }

        if (styleClass == null) {
            return shortPath;
        }

        return "<a href=\"#\" class=\"" + styleClass + "\">" + shortPath + "<span>" + realPath + "</span></a>";
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A valueDecorator must be used within a ColumnTag.");
        }

        ancestorTag.setDecorator(this);
        return EVAL_BODY_INCLUDE;
    }

    public void release() {
        styleClass = null;
        super.release();
    }

    public int getPostChars() {
        return postChars;
    }

    public void setPostChars(int postChars) {
        this.postChars = postChars;
    }

    public int getPreChars() {
        return preChars;
    }

    public void setPreChars(int preChars) {
        this.preChars = preChars;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }
}
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.rhq.enterprise.gui.legacy.util.TaglibUtils;

public class ShortenPathTag extends TagSupport {
    private int preChars;
    private int postChars;
    private String value = null;
    private String property = null;
    private boolean strict = false;

    public ShortenPathTag() {
        super();
    }

    public int doStartTag() throws JspException {
        String realValue;
        try {
            realValue = (String) ExpressionUtil.evalNotNull("spider", "value", getValue(), String.class, this,
                pageContext);
        } catch (NullAttributeException ne) {
            throw new JspTagException("typeId not found: " + ne);
        } catch (JspException je) {
            throw new JspTagException(je.toString());
        }

        value = TaglibUtils.shortenPath(realValue, preChars, postChars, strict);

        if (property != null) {
            pageContext.setAttribute(property, value);
        }

        pageContext.setAttribute("wasShortened", new Boolean(!value.equals(realValue)));

        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        try {
            if (property == null) {
                pageContext.getOut().println(value);
            }
        } catch (java.io.IOException e) {
            throw new JspException(e);
        }

        return super.doEndTag();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getProperty() {
        return this.property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int getPreChars() {
        return this.preChars;
    }

    public void setPreChars(int preChars) {
        this.preChars = preChars;
    }

    public int getPostChars() {
        return this.postChars;
    }

    public void setPostChars(int postChars) {
        this.postChars = postChars;
    }

    public boolean getStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
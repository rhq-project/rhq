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

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.rhq.core.clientapi.util.StringUtil;

public class RemovePrefixTag extends TagSupport {
    private String prefix = null;
    private String value = null;

    public RemovePrefixTag() {
        super();
    }

    public int doStartTag() throws JspException {
        String realPrefix;
        String realValue;
        try {
            realPrefix = (String) ExpressionUtil.evalNotNull("spider", "prefix", getPrefix(), String.class, this,
                pageContext);
            realValue = (String) ExpressionUtil.evalNotNull("spider", "value", getValue(), String.class, this,
                pageContext);
        } catch (NullAttributeException ne) {
            throw new JspTagException("typeId not found: " + ne);
        } catch (JspException je) {
            throw new JspTagException(je.toString());
        }

        value = StringUtil.removePrefix(realValue, realPrefix);
        try {
            pageContext.getOut().println(value);
        } catch (IOException e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
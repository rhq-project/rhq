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
/**
 * This is an abstract class that most tags should inherit from, it provides a number of utility methods that allow tags
 * to read in a template or multiple template files from the web/templates directory, and use those templates as
 * flexible StringBuffers that reread themselves when their matching file changes, etc...
 */
package org.rhq.enterprise.gui.legacy.taglib.display;

import java.io.IOException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

public abstract class TemplateTag extends BodyTagSupport {
    public void write(String val) throws JspTagException {
        try {
            JspWriter out = pageContext.getOut();
            out.write(val);
        } catch (IOException e) {
            throw new JspTagException("Writer Exception: " + e);
        }
    }

    public void write(StringBuffer val) throws JspTagException {
        this.write(val.toString());
    }

    public void write(StringBuilder val) throws JspTagException {
        this.write(val.toString());
    }
}
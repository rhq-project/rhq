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
import javax.servlet.jsp.JspWriter;
import org.rhq.enterprise.gui.legacy.Constants;

/**
 * return the link to the help system.
 */
public class HelpTag extends VarSetterBaseTag {
    //----------------------------------------------------public methods
    public int doStartTag() throws JspException {
        JspWriter output = pageContext.getOut();

        // Retrieve the context and compose the help URL
        String helpContext = (String) pageContext.getRequest().getAttribute(Constants.PAGE_TITLE_KEY);
        String helpURL = (String) pageContext.getServletContext().getAttribute(Constants.HELP_BASE_URL_KEY);

        if (helpContext != null) {
            helpURL = helpURL + helpContext;
        }

        try {
            output.print(helpURL);
        } catch (IOException e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }
}
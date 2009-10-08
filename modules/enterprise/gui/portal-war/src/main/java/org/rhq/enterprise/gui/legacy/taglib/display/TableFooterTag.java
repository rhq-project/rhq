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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TableFooterTag extends BodyTagSupport {
    /**
     * logger.
     */
    private static Log log = LogFactory.getLog(TableFooterTag.class);

    /**
     * is this the first iteration?
     */
    private boolean firstIteration;

    /**
     * @see javax.servlet.jsp.tagext.Tag#doEndTag()
     */
    public int doEndTag() throws JspException {
        if (this.firstIteration) {
            TableTag tableTag = (TableTag) findAncestorWithClass(this, TableTag.class);

            if (tableTag == null) {
                throw new JspException("Can not use footer tag outside of a " + "TableTag. Invalid parent = null");
            }

            // add column header only once
            log.debug("first call to doEndTag, setting footer");

            if (getBodyContent() != null) {
                tableTag.setFooter(getBodyContent().getString());
            }

            this.firstIteration = false;
        }

        return EVAL_PAGE;
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public int doStartTag() throws JspException {
        TableTag tableTag = (TableTag) findAncestorWithClass(this, TableTag.class);

        if (tableTag == null) {
            throw new JspException("Can not use footer tag outside of a " + "TableTag. Invalid parent = null");
        }

        // add column header only once
        this.firstIteration = tableTag.isFirstIteration();
        return this.firstIteration ? EVAL_BODY_BUFFERED : SKIP_BODY;
    }
}
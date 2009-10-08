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
import org.apache.struts.util.ResponseUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.BizappUtils;

/**
 * A JSP tag that formats and prints the "owner information" commonly displayed as part of the attributes of a resource.
 */
public class OwnerTag extends VarSetterBaseTag {
    //----------------------------------------------------instance variables

    /* the name of the scoped attribute that holds our user */
    private Object owner = null;

    //----------------------------------------------------constructors

    public OwnerTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the attribute in page, request, session or application scope that holds the <code>
     * OperationOwner</code> object.
     *
     * @param owner the name of the scoped attribute
     */
    public void setOwner(Object owner) {
        this.owner = owner;
    }

    /**
     * Process the tag, generating and printing the owner information.
     *
     * @exception JspException if the scripting variable can not be found or if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        /* XXX: would be nice if WebUser and AuthzSubjectValue
         * implemented a common interface or something
         */
        String username;
        String email;
        String full;
        if (owner instanceof WebUser) {
            WebUser webUser = (WebUser) owner;
            username = webUser.getUsername();
            email = webUser.getEmailAddress();
            full = BizappUtils.makeSubjectFullName(webUser.getFirstName(), webUser.getLastName());
        } else {
            Subject subject = (Subject) owner;
            username = subject.getName();
            email = subject.getEmailAddress();
            full = BizappUtils.makeSubjectFullName(subject.getFirstName(), subject.getLastName());
        }

        // if we have an email address:
        //   if we have a username, display full name and linked username
        //   else display linked full name
        // else
        //   display the full name
        //   if we have a username, display the username

        StringBuffer output = new StringBuffer();
        if ((email != null) && !email.equals("")) {
            if ((username != null) && (username.length() > 0)) {
                output.append(ResponseUtils.filter(full));
                output.append(" (<a href=\"mailto:");
                output.append(ResponseUtils.filter(email));
                output.append("\">");
                output.append(ResponseUtils.filter(username));
                output.append("</a>)");
            } else {
                output.append("<a href=\"mailto:");
                output.append(ResponseUtils.filter(email));
                output.append("\">");
                output.append(ResponseUtils.filter(full));
                output.append("</a>");
            }
        } else {
            output.append(ResponseUtils.filter(full));
            if ((username != null) && (username.length() > 0)) {
                output.append(" (");
                output.append(ResponseUtils.filter(username));
                output.append(")");
            }
        }

        setScopedVariable(output.toString());
        return SKIP_BODY;
    }

    /**
     * Release tag state.
     */
    public void release() {
        owner = null;
        super.release();
    }

    private Object evalAttr(String name, String value, Class type) throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("owner", name, value, type, this, pageContext);
    }
}
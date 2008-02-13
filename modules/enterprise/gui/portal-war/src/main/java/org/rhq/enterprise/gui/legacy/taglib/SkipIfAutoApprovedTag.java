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
import javax.servlet.jsp.tagext.BodyTagSupport;

public class SkipIfAutoApprovedTag extends BodyTagSupport {
    private String server;

    @Override
    public int doStartTag() throws JspException {
        //
        //        AiqServer server;
        //        try {
        //            server = (AiqServer) ExpressionUtil.evalNotNull("spider",
        //                                                         "server",
        //                                                         this.server,
        //                                                         AiqServer.class,
        //                                                         this,
        //                                                         pageContext );
        //        } catch (NullAttributeException ne) {
        //            throw new JspTagException("typeId not found: " + ne);
        //        } catch (JspException je) {
        //            throw new JspTagException( je.toString() );
        //        }
        //
        //        ServletContext ctx = pageContext.getServletContext();
        //        AppdefBoss appdefBoss = ContextUtils.getAppdefBoss(ctx);
        //        WebUser user
        //            = SessionUtils.getWebUser
        //            (((HttpServletRequest)pageContext.getRequest()).getSession());
        //        int sessionId = user.getSessionId();
        //        if (BizappUtils.isAutoApprovedServer(sessionId, appdefBoss, server)) {
        //            return SKIP_BODY;
        //        }
        return EVAL_BODY_INCLUDE;
    }

    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
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
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Generates a String representing the diff for an AIServer
 */
public class AutoInventoryServerDiff extends TagSupport {
    private String resource;

    public AutoInventoryServerDiff() {
        super();
    }

    @Override
    public int doStartTag() throws JspException {
        //        JspWriter output = pageContext.getOut();
        //        String diffString;
        //        AiqServer serverValue;
        //        try {
        //            serverValue = (AiqServer) ExpressionUtil.evalNotNull("spider",
        //                                                        "resource",
        //                                                        getResource(),
        //                                                        AiqServer.class,
        //                                                        this,
        //                                                        pageContext );
        //        }
        //        catch (NullAttributeException ne) {
        //            throw new JspTagException( " typeId not found");
        //        }
        //        catch (JspException je) {
        //            throw new JspTagException( je.toString() );
        //        }
        //
        //        diffString = AIQueueConstants.getServerDiffString(serverValue.getQueueStatus(),
        //                                               serverValue.getDiff());
        //        try{
        //            output.print( diffString);
        //        }
        //        catch(IOException e){
        //            throw new JspException(e);
        //        }
        return SKIP_BODY;
    }

    public String getResource() {
        return this.resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
}
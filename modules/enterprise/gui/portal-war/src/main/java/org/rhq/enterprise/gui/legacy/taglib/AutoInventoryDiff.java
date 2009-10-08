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
 * Generates a String representing the diff for an AIPlatform
 */
public class AutoInventoryDiff extends TagSupport {
    private String resource;

    public AutoInventoryDiff() {
        super();
    }

    @Override
    public int doStartTag() throws JspException {
        //        JspWriter output = pageContext.getOut();
        //        String diffString;
        //        AiqPlatform platformValue;
        //        try {
        //            platformValue = (AiqPlatform) ExpressionUtil.evalNotNull("spider",
        //                                                        "resource",
        //                                                        getResource(),
        //                                                        AiqPlatform.class,
        //                                                        this,
        //                                                        pageContext );
        //        }
        //        catch (NullAttributeException ne) {
        //            throw new JspTagException("typeId not found: " + ne);
        //        }
        //        catch (JspException je) {
        //            throw new JspTagException( je.toString() );
        //        }
        //
        //        diffString = AIQueueConstants.getPlatformDiffString(platformValue.getQueueStatus(),
        //                                               platformValue.getDiff());
        //        try{
        //            output.print(diffString);
        //        } catch(IOException e) {
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
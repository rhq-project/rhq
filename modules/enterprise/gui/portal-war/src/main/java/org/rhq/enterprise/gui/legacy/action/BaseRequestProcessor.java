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
package org.rhq.enterprise.gui.legacy.action;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.TilesRequestProcessor;
import org.rhq.enterprise.gui.legacy.KeyConstants;

/**
 * An <code>RequestProcessor</code> subclass that adds properties to the request for help and page name these properties
 * live in struts-config.xml
 */
public class BaseRequestProcessor extends TilesRequestProcessor {
    protected ActionForward processActionPerform(HttpServletRequest request, HttpServletResponse response,
        Action action, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
        BaseActionMapping smapping = null;
        try {
            smapping = (BaseActionMapping) mapping;
            if (smapping.getTitle() != null) {
                request.setAttribute(KeyConstants.PAGE_TITLE_KEY, smapping.getTitle());
            }

            return (action.execute(mapping, form, request, response));
        } catch (Exception e) {
            return (processException(request, response, e, form, mapping));
        }
    }
}
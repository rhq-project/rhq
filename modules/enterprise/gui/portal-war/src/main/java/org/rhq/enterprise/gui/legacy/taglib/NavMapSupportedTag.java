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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Set a scoped variable containing a boolean flag as to whether or not the running server and current page context both
 * support the navigation map functionality.
 */
public class NavMapSupportedTag extends VarSetterBaseTag {
    private Log log = LogFactory.getLog(NavMapSupportedTag.class.getName());

    @Override
    public final int doStartTag() throws JspException {
        //        try {
        //            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        //            ServletContext ctx = pageContext.getServletContext();
        //            AppdefBoss ab = ContextUtils.getAppdefBoss(ctx);
        //
        //            boolean navMapSupported = true;
        //
        //            // first try to get the resource ids as eids, then as rid / type
        //            AppdefEntityID[] eids = null;
        //            try {
        //                eids = RequestUtils.getEntityIds(request);
        //            } catch (ParameterNotFoundException e) {
        //                // either an auto-group of platforms or an individual resource
        //                try {
        //                    eids = new AppdefEntityID[] {
        //                            RequestUtils.getEntityId(request) };
        //                } catch (ParameterNotFoundException pnfe) {
        //                    // auto-group of platforms
        //                    eids = null;
        //                }
        //            }
        //
        //            if (null == eids) {
        //                // auto-group of platforms
        //                navMapSupported = true;
        //            } else {
        //                if (0 == eids.length) {
        //                    // ERROR, not supported
        //                    log.error("Couldn't find any entity ids.  Assuming NavMap not supported.");
        //                    navMapSupported = false;
        //                } else if (1 == eids.length) {
        //                    if ( AppdefEntityConstants.APPDEF_TYPE_GROUP == eids[0].getType() ) {
        //                        try {
        //                            int sessionId = RequestUtils.getSessionId(request).intValue();
        //                            AppdefGroupValue group = ab.findGroup(sessionId, eids[0]);
        //
        //                            if ( group.isGroupAdhoc() ) {
        //                                // mixed-group, not supported
        //                                navMapSupported = false;
        //                            } else {
        //                                // compatible-group or cluster, supported
        //                                navMapSupported = true;
        //                            }
        //                        } catch (SessionNotFoundException e) {
        //                            // ERROR, not supported
        //                            log.error("Session not found.  Assuming NavMap not supported.", e);
        //                            navMapSupported = false;
        //                        } catch (SessionTimeoutException e) {
        //                            // ERROR, not supported
        //                            log.error("Session timeout.  Assuming NavMap not supported.", e);
        //                            navMapSupported = false;
        //                        } catch (PermissionException e) {
        //                            // ERROR, not supported
        //                            log.error("No rights to this group.  Assuming NavMap not supported.", e);
        //                            navMapSupported = false;
        //                        } catch (AppdefGroupNotFoundException e) {
        //                            // ERROR, not supported
        //                            log.error("Group not found.  Assuming NavMap not supported.", e);
        //                            navMapSupported = false;
        //                        } catch (ServletException e) {
        //                            // ERROR, not supported
        //                            log.error("Couldn't check group type w/o session id.  Assuming NavMap not supported.", e);
        //                            navMapSupported = false;
        //                        }
        //
        //                    } else {
        //                        // non-group, supported
        //                        navMapSupported = true;
        //                    }
        //                } else {
        //                    // auto-group
        //                    navMapSupported = true;
        //                }
        //            }
        //
        //            // now check to see if the server supports the navigation map
        //            navMapSupported = navMapSupported && ab.isNavMapSupported();
        //            setScopedVariable( new Boolean(navMapSupported) );
        //        } catch (RemoteException e) {
        //            log.error("Couldn't interact with bizapp layer.  Assuming NavMap not supported.", e);
        //            setScopedVariable(Boolean.FALSE);
        //        }
        return SKIP_BODY;
    }
}

// EOF

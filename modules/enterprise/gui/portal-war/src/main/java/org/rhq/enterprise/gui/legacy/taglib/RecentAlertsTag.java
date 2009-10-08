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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>A JSP tag that will get the recent alerts for a user and put them in the request context.</p>
 *
 * <p>Attributes:
 *
 * <table border="1">
 *   <tr>
 *     <th>name</th>
 *     <th>required</th>
 *     <th>default</th>
 *   </tr>
 *   <tr>
 *     <td>var</th>
 *     <th>true</th>
 *     <th>N/A</th>
 *   </tr>
 *   <tr>
 *     <td>sizeVar</th>
 *     <th>true</th>
 *     <th>N/A</th>
 *   </tr>
 *   <tr>
 *     <td>maxAlerts</th>
 *     <th>false</th>
 *     <th>2</th>
 *   </tr>
 * </table>
 */
public class RecentAlertsTag extends TagSupport {
    Log log = LogFactory.getLog(RecentAlertsTag.class.getName());

    //----------------------------------------------------instance variables

    private String var;
    private String sizeVar;
    private int maxAlerts = 2;

    //----------------------------------------------------constructors

    public RecentAlertsTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the request attribute that should hold the list of alerts.
     *
     * @param var the name of the request attribute
     */
    public void setVar(String var) {
        this.var = var;
    }

    /**
     * Set the name of the request attribute that should hold the size of the list of alerts.
     *
     * @param sizeVar the name of the request attribute
     */
    public void setSizeVar(String sizeVar) {
        this.sizeVar = sizeVar;
    }

    /**
     * Set the max number of alerts to get.
     *
     * @param maxAlerts the max number of alerts
     */
    public void setMaxAlerts(String maxAlerts) {
        this.maxAlerts = Integer.parseInt(maxAlerts);
    }

    /**
     * Process the tag, generating and formatting the list.
     *
     * @exception JspException if the scripting variable can not be found or if there is an error processing the tag
     */
    @Override
    public final int doStartTag() throws JspException {
        //        try {
        //            HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        //            ServletContext ctx = pageContext.getServletContext();
        //
        //            // Get two most recent events.
        //            AppdefBoss ab = ContextUtils.getAppdefBoss(ctx);
        //            EventsBoss eb = ContextUtils.getEventsBoss(ctx);
        //            int sessionId = RequestUtils.getSessionId(request);
        //            List<Alert> alerts = eb.findUserAlerts(sessionId);
        //            if ( log.isTraceEnabled() ) {
        //                log.trace("found " + alerts.size() + " recent alerts");
        //            }
        //
        //            List recentAlertBeans = new ArrayList();
        //            Iterator alertIter = alerts.iterator();
        //            for (int i = 0; i < maxAlerts && alertIter.hasNext(); i++) {
        //                Alert alert = (Alert) alertIter.next();
        //                String resourceName;
        //                AlertDefinition alertDef;
        //                AppdefEntityID adeId = null;
        //
        //                try {
        ////                    alertDef = eb.getAlertDefinition(sessionId, alert.getAlertDef().getId());
        ////                    adeId = new AppdefEntityID( alertDef.getAppdefType(),
        ////                                                alertDef.getAppdefId() );
        //
        //                    AppdefResourceValue resource = ab.findById(sessionId, adeId);
        //                    resourceName = resource.getName();
        //                } catch (AppdefEntityNotFoundException e) {
        //                    // it's okay -- maybe the resource was deleted
        //                    if ( log.isDebugEnabled() ) {
        //                        log.debug("Resource not found: " + adeId, e);
        //                    }
        //                    continue;
        //                } catch (PermissionException e) {
        //                    continue;
        //                }
        //               alertDef = null;
        //
        //                recentAlertBeans.add(0,
        //                    new RecentAlertBean(alert.getId(),
        //                                        alert.getCtime(),
        //                                        alert.getAlertDefinition().getId(),
        //                                        alertDef.getName(),
        //                                        alertDef.getPriority(),
        //                                        null, //alertDef.getAppdefId(),
        //                                        null, //alertDef.getAppdefType(),
        //                                        resourceName));
        //            }
        //
        //            RecentAlertBean[] recentAlerts =
        //                (RecentAlertBean[]) recentAlertBeans.toArray(new RecentAlertBean[0]);
        //
        //            request.setAttribute(var, recentAlerts);
        //            request.setAttribute(sizeVar, recentAlerts.length);
        //
        return SKIP_BODY;
        ////        } catch (FinderException e) {
        ////            throw new JspTagException( e.getMessage() );
        //        } catch (SessionNotFoundException e) {
        //            throw new JspTagException( e.getMessage() );
        //        } catch (SessionTimeoutException e) {
        //            throw new JspTagException( e.getMessage() );
        //        } catch (RemoteException e) {
        //            throw new JspTagException( e.getMessage() );
        //        } catch (ServletException e) {
        //            throw new JspTagException( e.getMessage() );
        //        } catch (PermissionException e) {
        //            throw new JspTagException( e.getMessage() );
        //        }
    }

    @Override
    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        maxAlerts = 2;
        var = null;
        sizeVar = null;
        super.release();
    }
}

// EOF

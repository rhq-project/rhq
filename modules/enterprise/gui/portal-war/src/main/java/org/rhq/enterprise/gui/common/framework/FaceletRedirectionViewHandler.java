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
package org.rhq.enterprise.gui.common.framework;

import java.io.IOException;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.sun.facelets.FaceletViewHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;

/**
 * @author Joseph Marques
 */
public class FaceletRedirectionViewHandler extends FaceletViewHandler {

    private static Log log = LogFactory.getLog(FaceletRedirectionViewHandler.class);

    public FaceletRedirectionViewHandler(ViewHandler handler) {
        super(handler);
    }

    @Override
    public String getActionURL(FacesContext facesContext, String viewId) {
        // Evaluate any EL variables that are contained in the view id.
        // (e.g. "/rhq/resource/artifact/view.xhtml?id=#{param.id}")
        ValueExpression valueExpression = FacesExpressionUtility.createValueExpression(viewId, String.class);
        String actionURL = FacesExpressionUtility.getValue(valueExpression, String.class);
        return actionURL;
    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException {
        long monitorId = HibernatePerformanceMonitor.get().start();
        super.renderView(context, viewToRender);
        HibernatePerformanceMonitor.get().stop(monitorId, "URL " + getURL(viewToRender));
    }

    private String getURL(UIViewRoot viewToRender) {
        StringBuilder results = new StringBuilder(viewToRender.getViewId());

        boolean first = true;
        for (Map.Entry<String, Object> urlParam : viewToRender.getAttributes().entrySet()) {
            if (first) {
                results.append('?');
                first = false;
            } else {
                results.append('&');
            }
            results.append(urlParam.getKey()).append("=").append(urlParam.getValue());
        }

        return results.toString();
    }

    @Override
    protected void handleRenderException(FacesContext context, Exception ex) throws IOException, ELException,
        FacesException {
        try {
            if (context.getViewRoot().getViewId().equals("/rhq/common/error.xhtml")) {
                /*
                 * This is to protect from infinite redirects if the error page itself 
                 * has an error; in this case, revert to the default error handling, 
                 * which should provide extra context information to debug the issue
                 */
                log.error("Redirected back to ourselves, there must be a problem with the error.xhtml page", ex);
                super.handleRenderException(context, ex); // normal, ugly error page used to diagnose the issue
                return; // return early, to prevent infinite redirects back to ourselves
            }

            // squirrel the exception away in the session so it's maintained across the redirect boundary
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            Map<String, Object> sessionMap = externalContext.getSessionMap();
            sessionMap.put("GLOBAL_RENDER_ERROR", ex);

            FacesContextUtility.getResponse().sendRedirect("/rhq/common/error.xhtml");
        } catch (IOException ioe) {
            log.fatal("Could not process redirect to handle application error", ioe);
        }
    }

}
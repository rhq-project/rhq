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

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

import org.rhq.core.gui.util.FacesContextUtility;

/**
 * This phase listener is used to inject the HTTP headers into the response
 * for all JSF pages.
 * 
 * Currently we inject the headers for immediate cache expiry.
 * 
 * @author Lukas Krejci
 */
public class HttpHeadersInjector implements PhaseListener {

    private static final long serialVersionUID = 1411618613425109468L;

    public void afterPhase(PhaseEvent event) {
        //nothing done in here
    }

    /**
     * We insert all the HTTP headers into the response before
     * the RENDER_RESPONSE phase begins so that the headers are 
     * output before any other data.
     */
    public void beforePhase(PhaseEvent event) {
        Object r = FacesContextUtility.getFacesContext().getExternalContext().getResponse();

        //XXX should we care about the possible portlet responses?
        if (r instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse) r;
            response.setHeader("Expires", "-1");
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Max-Age", "0");

            // no-store in here forces Firefox to behave correctly and
            // not store anything in the cache. Only then the back button 
            // correctly reloads the page.
            response.setHeader("Cache-Control", "no-store");
        }
    }

    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }

}

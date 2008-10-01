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

import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * A phase listener that redirects the user to the login page if their
 * session has timed out
 *
 * @author Joseph Marques
 */
public class SessionTimeoutPhaseListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    public void beforePhase(PhaseEvent e) {
        FacesContext facesContext = e.getFacesContext();
        ExternalContext externalContext = facesContext.getExternalContext();
        if (externalContext.getSession(false) == null) {
            Application application = facesContext.getApplication();
            NavigationHandler handler = application.getNavigationHandler();
            handler.handleNavigation(facesContext, "", "sessionExpired");
        }
    }

    public void afterPhase(PhaseEvent arg0) {
        // no-op
    }

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }
}

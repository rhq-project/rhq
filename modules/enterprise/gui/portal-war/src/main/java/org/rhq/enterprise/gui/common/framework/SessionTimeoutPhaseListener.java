package org.rhq.enterprise.gui.common.framework;

import javax.faces.application.Application;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

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

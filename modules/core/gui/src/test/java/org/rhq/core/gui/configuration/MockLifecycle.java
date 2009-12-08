package org.rhq.core.gui.configuration;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

public class MockLifecycle extends Lifecycle {

    @Override
    public void addPhaseListener(PhaseListener listener) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void execute(FacesContext context) throws FacesException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public PhaseListener[] getPhaseListeners() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void removePhaseListener(PhaseListener listener) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void render(FacesContext context) throws FacesException {
        throw new RuntimeException("Function not implemented");

    }

}

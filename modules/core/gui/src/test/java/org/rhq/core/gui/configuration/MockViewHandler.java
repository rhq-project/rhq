package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.util.Locale;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

public class MockViewHandler extends ViewHandler {

    @Override
    public Locale calculateLocale(FacesContext context) {
        throw new RuntimeException("Function not implemented");
    }

    @Override
    public String calculateRenderKitId(FacesContext context) {
        throw new RuntimeException("Function not implemented");
    }

    @Override
    public UIViewRoot createView(FacesContext context, String viewId) {
        throw new RuntimeException("Function not implemented");
    }

    @Override
    public String getActionURL(FacesContext context, String viewId) {
        throw new RuntimeException("Function not implemented");
    }

    @Override
    public String getResourceURL(FacesContext context, String path) {
        return "uri://" + path;
    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException {
        throw new RuntimeException("Function not implemented");
    }

    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId) {
        throw new RuntimeException("Function not implemented");
    }

    @Override
    public void writeState(FacesContext context) throws IOException {
        throw new RuntimeException("Function not implemented");
    }

}

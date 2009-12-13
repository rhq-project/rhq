package org.rhq.core.gui.configuration;

import java.util.Iterator;

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;

import com.sun.faces.renderkit.RenderKitImpl;

public class MockFacesContext extends FacesContext {

    private ExternalContext externalContext;
    private MockResponseWriter responseWriter;

    public MockFacesContext(ExternalContext externalContext, MockResponseWriter responseWriter) {
        this.externalContext = externalContext;
        this.responseWriter = responseWriter;
        FacesContext.setCurrentInstance(this);
    }

    @Override
    public void addMessage(String clientId, FacesMessage message) {
        throw new RuntimeException("Function not implemented");
    }

    MockApplication mockApp;

    @Override
    public Application getApplication() {
        if (null == mockApp) {
            mockApp = new MockApplication();
        }
        return mockApp;
    }

    @Override
    public Iterator<String> getClientIdsWithMessages() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ExternalContext getExternalContext() {
        return externalContext;

    }

    @Override
    public Severity getMaximumSeverity() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<FacesMessage> getMessages() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<FacesMessage> getMessages(String clientId) {
        throw new RuntimeException("Function not implemented");

    }

    RenderKit renderKit = new MockRenderKit();

    @Override
    public RenderKit getRenderKit() {
        if (null == renderKit) {
            renderKit = new RenderKitImpl();
            renderKit.addRenderer("rhq", ConfigRenderer.class.getName(), new ConfigRenderer());
        }
        return renderKit;
    }

    @Override
    public boolean getRenderResponse() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public boolean getResponseComplete() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ResponseStream getResponseStream() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ResponseWriter getResponseWriter() {
        return responseWriter;
    }

    @Override
    public UIViewRoot getViewRoot() {
        return null;
        //throw new RuntimeException("Function not implemented");

    }

    @Override
    public void release() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void renderResponse() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void responseComplete() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setResponseStream(ResponseStream responseStream) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setResponseWriter(ResponseWriter responseWriter) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void setViewRoot(UIViewRoot root) {
        throw new RuntimeException("Function not implemented");

    }

}

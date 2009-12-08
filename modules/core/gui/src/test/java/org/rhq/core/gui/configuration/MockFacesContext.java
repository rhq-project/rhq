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

public class MockFacesContext extends FacesContext {

    private ExternalContext externalContext;
    private Object responseCompleteWriter;
    private MockResponseWriter mockResponseWriter;

    public MockFacesContext(ExternalContext externalContext, ResponseWriter responseWriter) {
        this.externalContext = externalContext;
        this.responseCompleteWriter = responseWriter;
    }

    @Override
    public void addMessage(String clientId, FacesMessage message) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Application getApplication() {
        throw new RuntimeException("Function not implemented");

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

    @Override
    public RenderKit getRenderKit() {
        throw new RuntimeException("Function not implemented");

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
        if (null == mockResponseWriter) {
            mockResponseWriter = new MockResponseWriter();
        }
        return mockResponseWriter;
    }

    @Override
    public UIViewRoot getViewRoot() {
        throw new RuntimeException("Function not implemented");

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

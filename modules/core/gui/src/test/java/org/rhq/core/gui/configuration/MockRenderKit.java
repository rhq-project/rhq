package org.rhq.core.gui.configuration;

import java.io.OutputStream;
import java.io.Writer;

import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import javax.faces.render.ResponseStateManager;

public class MockRenderKit extends RenderKit {

    @Override
    public void addRenderer(String family, String rendererType, Renderer renderer) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ResponseStream createResponseStream(OutputStream out) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public ResponseWriter createResponseWriter(Writer writer, String contentTypeList, String characterEncoding) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Renderer getRenderer(String family, String rendererType) {
        try {
            return (Renderer) Class.forName(rendererType).newInstance();
        } catch (Exception e) {
            return null;
        }

    }

    @Override
    public ResponseStateManager getResponseStateManager() {
        throw new RuntimeException("Function not implemented");

    }

}

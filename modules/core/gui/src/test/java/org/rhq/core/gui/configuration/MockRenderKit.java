package org.rhq.core.gui.configuration;

import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;

import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import javax.faces.render.ResponseStateManager;

import com.sun.faces.renderkit.html_basic.GroupRenderer;
import com.sun.faces.renderkit.html_basic.OutputMessageRenderer;
import com.sun.faces.renderkit.html_basic.TextRenderer;

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

    HashSet<String> noRendererSet;

    public HashSet<String> getNoRendererSet() {
        if (null == noRendererSet) {
            noRendererSet = new HashSet<String>();
            noRendererSet.add("javax.faces.HtmlCommandLink");
            noRendererSet.add("javax.faces.Form");
            noRendererSet.add("javax.faces.Textarea");
        }
        return noRendererSet;
    }

    HashMap<String, Class> rendererMap;

    @Override
    public Renderer getRenderer(String family, String rendererType) {

        if (getNoRendererSet().contains(rendererType))
            return null;

        try {
            if (null == rendererMap) {
                rendererMap = new HashMap<String, Class>();
                rendererMap.put("javax.faces.Text", TextRenderer.class);
                rendererMap.put("javax.faces.Format", OutputMessageRenderer.class);
                rendererMap.put("javax.faces.Group", GroupRenderer.class);
                rendererMap.put(ConfigRenderer.class.getName(), ConfigRenderer.class);
                rendererMap.put("javax.faces.Link", com.sun.faces.renderkit.html_basic.CommandLinkRenderer.class);
                rendererMap.put("javax.faces.Image", com.sun.faces.renderkit.html_basic.ImageRenderer.class);
                rendererMap.put("javax.faces.Grid", com.sun.faces.renderkit.html_basic.GridRenderer.class);

            }
            Class c = rendererMap.get(rendererType);
            if (c == null) {
                System.out.println("MockRenderKit request for unsupported type " + rendererType);
            }
            return (Renderer) c.newInstance();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ResponseStateManager getResponseStateManager() {
        throw new RuntimeException("Function not implemented");

    }

}

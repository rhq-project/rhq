package org.rhq.core.gui.configuration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;

public class MockRenderKitFactory extends RenderKitFactory {

    Map<String, RenderKit> kits = new HashMap<String, RenderKit>();

    @Override
    public void addRenderKit(String renderKitId, RenderKit renderKit) {
        kits.put(renderKitId, renderKit);
    }

    @Override
    public RenderKit getRenderKit(FacesContext context, String renderKitId) {
        return kits.get(renderKitId);

    }

    @Override
    public Iterator<String> getRenderKitIds() {
        return kits.keySet().iterator();
    }

}

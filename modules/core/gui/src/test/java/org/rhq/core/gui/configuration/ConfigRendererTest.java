package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.util.HashMap;

import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.render.RenderKitFactory;

import com.sun.faces.context.FacesContextImpl;

import org.ajax4jsf.context.AjaxContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.gui.util.FacesContextUtility;

public class ConfigRendererTest {

    RenderKitFactory rkf;
    ConfigUIComponent configUIComponent;
    ConfigRenderer configRenderer;
    FacesContext facesContext;
    ExternalContext externalContext;
    AjaxContext ajaxContext;
    MockResponseWriter mockResponseWriter;
    Lifecycle lifecycle;

    @BeforeMethod
    protected void setUp() throws Exception {
        configUIComponent = new ConfigUIComponent();
        configRenderer = new ConfigRenderer();

        externalContext = new MockExternalContext();
        mockResponseWriter = new MockResponseWriter();
        facesContext = new MockFacesContext(externalContext, mockResponseWriter);
        FacesContext.getCurrentInstance();

        configUIComponent.setId("ID");

        ajaxContext = AjaxContext.getCurrentInstance(facesContext);
        lifecycle = new MockLifecycle();

        ApplicationFactory af = new MockApplicationFactory();

        FactoryFinder.setFactory(FactoryFinder.RENDER_KIT_FACTORY, MockRenderKitFactory.class.getName());
        FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY, MockApplicationFactory.class.getName());

        HashMap<Object, Object> map = new HashMap();
        facesContext.getExternalContext().getRequestMap().put("com.sun.faces.util.RequestStateManager", map);
        FacesContextImpl impl = new FacesContextImpl(externalContext, lifecycle);
        map.put("com.sun.faces.FacesContextImpl", impl);
        ValueExpression binding = new MockValueExpression();
        binding.setValue(FacesContextUtility.getFacesContext().getELContext(), new Configuration());
        configUIComponent.setValueExpression("configuration", binding);

    }

    @Test
    public void testDecode() {
        configUIComponent.setReadOnly(false);
        Assert.assertNotNull(configUIComponent.getConfiguration());
        configRenderer.decode(facesContext, configUIComponent);
    }

    @Test
    public void testEncode() throws IOException {
        configRenderer.encodeBegin(this.facesContext, configUIComponent);
        configRenderer.encodeChildren(facesContext, configUIComponent);
        configRenderer.encodeEnd(facesContext, configUIComponent);
        System.out.println(mockResponseWriter.stringWriter.getBuffer().toString());
    }

}

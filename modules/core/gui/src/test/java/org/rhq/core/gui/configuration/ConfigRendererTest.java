package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.faces.FactoryFinder;
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
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

public class ConfigRendererTest {

    RenderKitFactory rkf;
    ConfigUIComponent configUIComponent;
    ConfigRenderer configRenderer;
    FacesContext facesContext;
    ExternalContext externalContext;
    AjaxContext ajaxContext;
    MockResponseWriter mockResponseWriter;
    Lifecycle lifecycle;
    ConfigurationDefinition configurationDefinition;

    @BeforeMethod
    protected void setUp() throws Exception {
        //Order is pretty important here
        FactoryFinder.setFactory(FactoryFinder.RENDER_KIT_FACTORY, MockRenderKitFactory.class.getName());
        FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY, MockApplicationFactory.class.getName());

        externalContext = new MockExternalContext();
        lifecycle = new MockLifecycle();
        mockResponseWriter = new MockResponseWriter();
        configUIComponent = new ConfigUIComponent();
        configRenderer = new ConfigRenderer();

        FacesContextImpl impl = new FacesContextImpl(externalContext, lifecycle);
        HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put("com.sun.faces.FacesContextImpl", impl);

        //Whichever one gets created last wins the 
        //"I'm registered with thread local storage and you are not" game
        facesContext = new MockFacesContext(externalContext, mockResponseWriter);
        facesContext.getExternalContext().getRequestMap().put("com.sun.faces.util.RequestStateManager", map);
        ajaxContext = AjaxContext.getCurrentInstance(facesContext);

        configUIComponent.setId("ID");

        configUIComponent.setValueExpression("configuration", new MockValueExpression(new Configuration()));
        configUIComponent.setValueExpression("configurationDefinition",
            new MockValueExpression(buildConfigDefinition()));
    }

    private ConfigurationDefinition buildConfigDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("MockDef", "Mock Configuration definition");

        return def;
    }

    //@Test
    public void testUIComponentDecode() {
        configUIComponent.setRendererType(ConfigRenderer.class.getName());
        configUIComponent.decode(facesContext);
    }

    @Test
    public void testUIComponentEncode() throws IOException {
        configUIComponent.setRendererType(ConfigRenderer.class.getName());
        configUIComponent.encodeAll(facesContext);
    }

    //@Test
    public void testDecode() {
        configUIComponent.setReadOnly(false);
        Assert.assertNotNull(configUIComponent.getConfiguration());
        configRenderer.decode(facesContext, configUIComponent);
    }

    //@Test
    public void testEncode() throws IOException {
        configUIComponent.setReadOnly(true);
        configUIComponent.setPrevalidate(false);
        /*
         <onc:config 
         configurationDefinition="#{ExistingResourceConfigurationUIBean.configurationDefinition}"
         configuration="#{ExistingResourceConfigurationUIBean.configuration}"
         accessMapActionExpression="#{ExistingResourceConfigurationUIBean.accessMap}"
         readOnly="true"
         nullConfigurationDefinitionMessage="#{ExistingResourceConfigurationUIBean.nullConfigurationDefinitionMessage}"
         nullConfigurationMessage="#{ExistingResourceConfigurationUIBean.nullConfigurationMessage}"
         prevalidate="true"/>
         */

        configRenderer.encodeBegin(this.facesContext, configUIComponent);
        configRenderer.encodeChildren(facesContext, configUIComponent);
        configRenderer.encodeEnd(facesContext, configUIComponent);
        String s1 = mockResponseWriter.stringWriter.getBuffer().toString();

        mockResponseWriter.stringWriter = new StringWriter();

        configUIComponent.setReadOnly(false);
        configUIComponent.setPrevalidate(false);

        configRenderer.encodeBegin(this.facesContext, configUIComponent);
        configRenderer.encodeChildren(facesContext, configUIComponent);
        configRenderer.encodeEnd(facesContext, configUIComponent);
        String s2 = mockResponseWriter.stringWriter.getBuffer().toString();

        Assert.assertTrue(!s1.equals(s2));

        System.out.println(s1);

    }

}

package org.rhq.core.gui.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import org.rhq.core.template.TemplateEngine;

public class TemplateEngineConverter implements Converter {

    private final TemplateEngine templateEngine;

    public TemplateEngineConverter(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (null == value){
            return "";
        }        
        return templateEngine.replaceTokens(value);            
    }

    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value == null)return "";
        return templateEngine.replaceTokens(value.toString());                    
    }

}

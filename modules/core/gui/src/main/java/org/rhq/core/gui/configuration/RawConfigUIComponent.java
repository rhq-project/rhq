package org.rhq.core.gui.configuration;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;

public class RawConfigUIComponent extends UIComponentBase {

    @Override
    public void decode(FacesContext context) {
        super.decode(context);

    }

    @Override
    public String getFamily() {
        return "rhq";
    }

    Configuration configuration;
    ConfigurationDefinition configurationDefinition;
    FacesComponentIdFactory idFactory;

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
        for (UIComponent kid : getChildren()) {
            kid.encodeAll(context);
        }
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    public RawConfigUIComponent(Configuration configuration, ConfigurationDefinition configurationDefinition,
        FacesComponentIdFactory componentIdFactory) {

        this.configuration = configuration;
        this.configurationDefinition = configurationDefinition;
        this.idFactory = componentIdFactory;

        UIPanel rawPanel = FacesComponentUtility.addBlockPanel(this, idFactory, "");
        addToolbarIcons(rawPanel);

        HtmlPanelGrid grid = FacesComponentUtility.addPanelGrid(rawPanel, idFactory, "summary-props-table");
        grid.setParent(this);
        grid.setColumns(2);
        grid.setColumnClasses("raw-config-table");

        HtmlPanelGrid panelLeft = FacesComponentUtility.addPanelGrid(grid, idFactory, "summary-props-table");
        panelLeft.setBgcolor("#a4b2b9");
        panelLeft.setColumns(1);

        FacesComponentUtility.addOutputText(panelLeft, idFactory, "Raw Configurations Paths", "");

        int rawCount = 0;
        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            UIPanel nextPath = FacesComponentUtility.addBlockPanel(panelLeft, idFactory, "");
            HtmlCommandLink link = FacesComponentUtility.addCommandLink(nextPath, idFactory);

            FacesComponentUtility.addOutputText(link, idFactory, raw.getPath(), "");
            FacesComponentUtility.addParameter(link, idFactory, "path", raw.getPath());
            FacesComponentUtility.addParameter(link, idFactory, "whichRaw", Integer.toString(rawCount++));
            FacesComponentUtility.addParameter(link, idFactory, "showRaw", Boolean.TRUE.toString());
        }

        UIPanel panelRight = FacesComponentUtility.addBlockPanel(grid, idFactory, "summary-props-table");

        UIPanel editPanel = FacesComponentUtility.addBlockPanel(panelRight, idFactory, "summary-props-table");
        HtmlInputTextarea inputTextarea = new HtmlInputTextarea();
        editPanel.getChildren().add(inputTextarea);
        inputTextarea.setParent(editPanel);
        inputTextarea.setCols(80);
        inputTextarea.setRows(40);
        inputTextarea.setValue(configuration.getRawConfigurations().iterator().next().getContentString());
    }

    private void addToolbarIcons(UIPanel panelRight) {
        HtmlCommandLink fullScreenCommandLink = FacesComponentUtility.addCommandLink(panelRight, idFactory);
        FacesComponentUtility.addGraphicImage(fullScreenCommandLink, idFactory, "/images/save.png", "Save");
        FacesComponentUtility.addOutputText(fullScreenCommandLink, idFactory, "Save", "");

        HtmlCommandLink saveCommandLink = FacesComponentUtility.addCommandLink(panelRight, idFactory);
        FacesComponentUtility.addGraphicImage(saveCommandLink, idFactory, "/images/viewfullscreen.png", "FullScreen");
        FacesComponentUtility.addOutputText(saveCommandLink, idFactory, "Full Screen", "");

        HtmlCommandLink uploadCommandLink = FacesComponentUtility.addCommandLink(panelRight, idFactory);
        FacesComponentUtility.addGraphicImage(uploadCommandLink, idFactory, "/images/upload.png", "Upload");
        FacesComponentUtility.addOutputText(uploadCommandLink, idFactory, "Upload", "");

        HtmlCommandLink downloadCommandLink = FacesComponentUtility.addCommandLink(panelRight, idFactory);
        FacesComponentUtility.addGraphicImage(downloadCommandLink, idFactory, "/images/download.png", "download");
        FacesComponentUtility.addOutputText(downloadCommandLink, idFactory, "Download", "");
    }
}

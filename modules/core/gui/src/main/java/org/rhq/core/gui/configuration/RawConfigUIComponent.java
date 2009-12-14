package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import org.rhq.core.gui.util.FacesContextUtility;

public class RawConfigUIComponent extends UIComponentBase {

    private boolean readOnly;

    @Override
    public void decode(FacesContext context) {
        super.decode(context);
        setSelectedPath(FacesContextUtility.getOptionalRequestParameter("path"));

    }

    @Override
    public String getFamily() {
        return "rhq";
    }

    private Configuration configuration;
    private ConfigurationDefinition configurationDefinition;
    private FacesComponentIdFactory idFactory;
    private String selectedPath;
    private HtmlInputTextarea inputTextarea;
    private Map<String, RawConfiguration> rawMap;

    public String getSelectedPath() {
        if (null == selectedPath) {
            if (configuration.getRawConfigurations().iterator().hasNext()) {
                selectedPath = configuration.getRawConfigurations().iterator().next().getPath();
            } else {
                selectedPath = "";
            }
        }
        return selectedPath;
    }

    public void setSelectedPath(String selectedPath) {
        if (selectedPath == null)
            return;
        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            if (raw.getPath().equals(selectedPath)) {
                this.selectedPath = selectedPath;
            }
        }
    }

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
        FacesComponentIdFactory componentIdFactory, boolean readOnly) {

        this.configuration = configuration;
        this.configurationDefinition = configurationDefinition;
        this.idFactory = componentIdFactory;
        this.readOnly = readOnly;

        UIPanel rawPanel = FacesComponentUtility.addBlockPanel(this, idFactory, "");
        addToolbarIcons(rawPanel, configurationDefinition.getConfigurationFormat().isStructuredSupported());

        HtmlPanelGrid grid = FacesComponentUtility.addPanelGrid(rawPanel, idFactory, "summary-props-table");
        grid.setParent(this);
        grid.setColumns(2);
        grid.setColumnClasses("raw-config-table");

        HtmlPanelGrid panelLeft = FacesComponentUtility.addPanelGrid(grid, idFactory, "summary-props-table");
        panelLeft.setBgcolor("#a4b2b9");
        panelLeft.setColumns(1);

        FacesComponentUtility.addOutputText(panelLeft, idFactory, "Raw Configurations Paths", "");

        int rawCount = 0;

        ArrayList<String> configPathList = new ArrayList<String>();

        rawMap = new HashMap<String, RawConfiguration>();

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            configPathList.add(raw.getPath());
            rawMap.put(raw.getPath(), raw);
        }
        Collections.sort(configPathList);
        String oldDirname = "";
        for (String s : configPathList) {
            RawConfiguration raw = rawMap.get(s);
            String dirname = raw.getPath().substring(0, raw.getPath().lastIndexOf("/") + 1);
            String basename = raw.getPath().substring(raw.getPath().lastIndexOf("/") + 1, raw.getPath().length());
            if (!dirname.equals(oldDirname)) {
                FacesComponentUtility.addOutputText(panelLeft, idFactory, dirname, "");
                oldDirname = dirname;
            }
            UIPanel nextPath = FacesComponentUtility.addBlockPanel(panelLeft, idFactory, "");
            HtmlCommandLink link = FacesComponentUtility.addCommandLink(nextPath, idFactory);
            FacesComponentUtility.addOutputText(link, idFactory, "[]" + basename, "");
            FacesComponentUtility.addParameter(link, idFactory, "path", raw.getPath());
            FacesComponentUtility.addParameter(link, idFactory, "whichRaw", Integer.toString(rawCount++));
            FacesComponentUtility.addParameter(link, idFactory, "showRaw", Boolean.TRUE.toString());
        }

        UIPanel panelRight = FacesComponentUtility.addBlockPanel(grid, idFactory, "summary-props-table");

        UIPanel editPanel = FacesComponentUtility.addBlockPanel(panelRight, idFactory, "summary-props-table");
        inputTextarea = new HtmlInputTextarea();
        editPanel.getChildren().add(inputTextarea);
        inputTextarea.setParent(editPanel);
        inputTextarea.setCols(80);
        inputTextarea.setRows(40);
        inputTextarea.setValue(rawMap.get(getSelectedPath()).getContentString());
        inputTextarea.setReadonly(readOnly);
    }

    private void addToolbarIcons(UIPanel toolbarPanel, boolean supportsStructured) {

        if (readOnly) {
            HtmlCommandLink editLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
            FacesComponentUtility.addGraphicImage(editLink, idFactory, "/images/edit.png", "Edit");
            FacesComponentUtility.addOutputText(editLink, idFactory, "Edit", "");
            FacesComponentUtility.addParameter(editLink, idFactory, "showStructured", Boolean.FALSE.toString());
        } else {
            HtmlCommandLink saveLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
            FacesComponentUtility.addGraphicImage(saveLink, idFactory, "/images/save.png", "Save");
            FacesComponentUtility.addOutputText(saveLink, idFactory, "Save", "");
            FacesComponentUtility.addParameter(saveLink, idFactory, "showStructured", Boolean.FALSE.toString());
        }
        HtmlCommandLink saveCommandLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
        FacesComponentUtility.addGraphicImage(saveCommandLink, idFactory, "/images/viewfullscreen.png", "FullScreen");
        FacesComponentUtility.addOutputText(saveCommandLink, idFactory, "Full Screen", "");

        HtmlCommandLink uploadCommandLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
        FacesComponentUtility.addGraphicImage(uploadCommandLink, idFactory, "/images/upload.png", "Upload");
        FacesComponentUtility.addOutputText(uploadCommandLink, idFactory, "Upload", "");

        HtmlCommandLink downloadCommandLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
        FacesComponentUtility.addGraphicImage(downloadCommandLink, idFactory, "/images/download.png", "download");
        FacesComponentUtility.addOutputText(downloadCommandLink, idFactory, "Download", "");

        if (supportsStructured) {
            HtmlCommandLink toStructureLink = FacesComponentUtility.addCommandLink(toolbarPanel, idFactory);
            FacesComponentUtility.addGraphicImage(toStructureLink, idFactory, "/images/structured.png",
                "showStructured");
            FacesComponentUtility.addOutputText(toStructureLink, idFactory, "showStructured", "");
            FacesComponentUtility.addParameter(toStructureLink, idFactory, "showStructured", Boolean.FALSE.toString());
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        // TODO Auto-generated method stub
        super.encodeBegin(context);
        inputTextarea.setValue(rawMap.get(getSelectedPath()).getContentString());

    }
}

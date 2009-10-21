package org.rhq.enterprise.gui.configuration.resource;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.faces.component.UIComponent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

//@Name("RawConfigCollection")
//@Scope(ScopeType.CONVERSATION)
public class RawConfigCollection {

    UIComponent binding;
    private String selectedPath;
    private TreeMap<String, RawConfiguration> raws;
    private TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    protected ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    RawConfiguration current;
    private Configuration configuration;

    //This is for development, to prevent actually going to the EJBs.
    //It should be set to false prior to check in
    private static final boolean useMock = false;

    public UIComponent getBinding() {
        return binding;
    }

    public void setBinding(UIComponent binding) {
        this.binding = binding;
    }

    public void select(String s) {
        this.selectedPath = s;
        setCurrentPath(selectedPath);
    }

    public int getConfigId() {

        if (null == configuration) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int resourceId = EnterpriseFacesContextUtility.getResource().getId();
            AbstractResourceConfigurationUpdate configurationUpdate = this.configurationManager
                .getLatestResourceConfigurationUpdate(subject, resourceId);
            configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
        }
        return configuration.getId();
    }

    public TreeMap<String, RawConfiguration> getRaws() {

        if (null == raws) {
            raws = new TreeMap<String, RawConfiguration>();
            if (useMock) {
                populateRawsMock();
            } else {
                populateRaws();
            }
        }
        return raws;
    }

    private void populateRawsMock() {
        String[] files = { "/etc/mock/file1", "/etc/mock/file2", "/etc/mock/file2" };

        for (String file : files) {
            RawConfiguration raw = new RawConfiguration();
            raw.setPath(file);
            raw.setContents(("contents of file" + file).getBytes());
            raws.put(file, raw);
        }

    }

    private void populateRaws() {
        Collection<RawConfiguration> rawConfigurations = configurationManager
            .findRawConfigurationsByConfigurationId(getConfigId());
        for (RawConfiguration raw : rawConfigurations) {
            raws.put(raw.getPath(), raw);
        }
    }

    public RawConfigCollection() {
    }

    public RawConfiguration getCurrent() {
        if (null == current) {
            Iterator<RawConfiguration> iterator = getRaws().values().iterator();
            if (iterator.hasNext()) {
                current = iterator.next();
            } else {
                current = new RawConfiguration();
                current.setPath("/dev/null");
                current.setContents("".getBytes());
            }
        }
        return current;
    }

    public String getCurrentContents() {
        return new String(getCurrent().getContents());
    }

    public void setCurrentContents(String updated) {
        String u2 = updated.substring(2);
        String original = new String(getCurrent().getContents());
        if (!u2.equals(original)) {
            current = current.deepCopy();
            current.setContents(updated.getBytes());
            //TODO update other values like MD5
            modified.put(current.getPath(), current);
        }
    }

    public void setCurrentPath(String path) {
        RawConfiguration raw = modified.get(path);
        if (null == raw) {
            raw = getRaws().get(path);
        }

        if (null != raw) {
            current = raw;
        }
    }

    public String getCurrentPath() {
        return getCurrent().getPath();
    }

    public Object[] getPaths() {
        return getRaws().keySet().toArray();
    }

    public void setModified(RawConfiguration raw) {
        modified.put(raw.getPath(), raw);
    }

    public boolean isModified(String path) {
        return modified.keySet().contains(path);
    }
}

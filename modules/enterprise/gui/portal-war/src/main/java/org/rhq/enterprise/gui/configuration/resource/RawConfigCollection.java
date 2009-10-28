package org.rhq.enterprise.gui.configuration.resource;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.richfaces.event.UploadEvent;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Name("RawConfigCollection")
@Scope(ScopeType.CONVERSATION)
public class RawConfigCollection implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4837157548556168146L;
    private String selectedPath;
    private TreeMap<String, RawConfiguration> raws;
    private TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    private RawConfiguration current = null;

    public void fileUploadListener(UploadEvent event) throws Exception {
        setCurrentContents(new String(event.getUploadItem().getData()));
    }

    void setFileSize(int size) {
        System.out.println(size);
    }

    public void setData(byte[] data) {

        if (data != null) {
            setCurrentContents(data.toString());
        }

    }

    public void upload() {

    }

    @Create
    @Begin
    public void init() {

    }

    /**
     * These values are transient due to the need to save and restore the view.  
     * We can always fetch them again.
     */
    transient private ConfigurationManagerLocal configurationManager;
    transient private Configuration configuration = null;

    //This is for development, to prevent actually going to the EJBs.
    //It should be set to false prior to check in
    private static final boolean useMock = true;

    public RawConfigCollection() {
    }

    public void select(String s) {
        this.selectedPath = s;
        setCurrentPath(selectedPath);
    }

    public Configuration getConfiguration() {
        if (null == configuration) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int resourceId = EnterpriseFacesContextUtility.getResource().getId();
            AbstractResourceConfigurationUpdate configurationUpdate = this.getConfigurationManager()
                .getLatestResourceConfigurationUpdate(subject, resourceId);
            configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
        }

        return configuration;
    }

    public int getConfigId() {

        return getConfiguration().getId();
    }

    @End
    public void commit() {
        //        configurationManager.findRawConfigurationsByConfigurationId(getConfigId());   

        for (RawConfiguration raw : modified.values()) {
            getRaws().put(raw.getPath(), raw);
        }
        getConfigurationManager();
        //        getConfigurationManager().setRawConfigurations(configId, raws.values());

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
        String[] files = { "/etc/mock/file1", "/etc/mock/file2", "/etc/mock/file3", "/etc/mock/me/will/you",
            "/etc/mock/turtle/soup", "/etc/mock/mysmock/iclean/yourclock" };

        for (String file : files) {
            RawConfiguration raw = new RawConfiguration();
            raw.setPath(file);
            raw.setContents(("contents of file" + file).getBytes());
            raws.put(file, raw);
        }

    }

    private void populateRaws() {
        Collection<RawConfiguration> rawConfigurations = getConfigurationManager()
            .findRawConfigurationsByConfigurationId(getConfigId());
        for (RawConfiguration raw : rawConfigurations) {
            raws.put(raw.getPath(), raw);
        }
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

    private ConfigurationManagerLocal getConfigurationManager() {
        if (null == configurationManager) {
            configurationManager = LookupUtil.getConfigurationManager();
        }

        return configurationManager;
    }
}

package org.rhq.enterprise.gui.configuration.resource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Name("RawConfigCollection")
@Scope(ScopeType.SESSION)
/**
 * The backing class for all Web based activities for manipulating the set of
 * raw configuration files associated with a resource
 */
public class RawConfigCollection implements Serializable {

    /*This is for development, to prevent actually going to the EJBs.
    TODO It should be set to false prior to check in*/
    private static final boolean useMock = true;

    /**
     * These values are transient due to the need to save and restore the view.  
     * We can always fetch them again.
     */
    transient private ConfigurationManagerLocal configurationManager;
    transient private Configuration configuration = null;

    private final Log log = LogFactory.getLog(RawConfigCollection.class);
    private int resourceId = 0;
    private static final long serialVersionUID = 4837157548556168146L;
    private String selectedPath;
    private TreeMap<String, RawConfiguration> raws;
    private TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    private RawConfiguration current = null;
    private ConfigurationDefinition configurationDefinition = null;

    public RawConfigCollection() {
    }

    void nullify() {
        resourceId = 0;
        selectedPath = null;
        raws = null;
        modified.clear();
        current = null;
        configurationDefinition = null;
    }

    public void fileUploadListener(UploadEvent event) throws Exception {
        File uploadFile;
        log.error("fileUploadListener called");
        uploadFile = event.getUploadItem().getFile();
        if (uploadFile != null) {
            log.debug("fileUploadListener got file named " + event.getUploadItem().getFileName());
            log.debug("content type is " + event.getUploadItem().getContentType());
            if (uploadFile != null) {
                try {
                    FileReader fileReader = new FileReader(uploadFile);
                    char[] buff = new char[1024];
                    StringBuffer stringBuffer = new StringBuffer();
                    for (int count = fileReader.read(buff); count != -1; count = fileReader.read(buff)) {
                        stringBuffer.append(buff, 0, count);
                    }
                    setCurrentContents(stringBuffer.toString());
                } catch (IOException e) {
                    log.error("problem reading uploaded file", e);
                }
            }
        }
    }

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "action" value 
     */
    public void upload() {
    }

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "save" icon from the full screen page
     */
    public String update() {
        log.error("update called");

        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    /**
     * Hack Alert.  This bean needs to be initialized on one of the pages that has id or resourceId set
     * In order to capture the resource.  It will then Keep track of that particular resources until
     * commit is called.  Ideally, this should be a conversation scoped bean, but that has other issues
     */
    @Create
    @Begin
    public void init() {
        //        resourceId = EnterpriseFacesContextUtility.getResource().getId();
    }

    /**
    *      
    * @return the id associated with the resource.  
    * The value Cached in order to be available on the upload page, 
    * where seeing the resource id conflicts with the rich upload tag.
    */
    public int getResourceId() {
        if (resourceId == 0) {
            resourceId = EnterpriseFacesContextUtility.getResource().getId();
        }
        return resourceId;
    }

    /**
     * Indicates which of the raw configuration files is currently selected.
     * @param s
     */
    public void select(String s) {
        this.selectedPath = s;
        setCurrentPath(selectedPath);
    }

    public Configuration getConfiguration() {
        if (null == configuration) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            AbstractResourceConfigurationUpdate configurationUpdate = this.getConfigurationManager()
                .getLatestResourceConfigurationUpdate(subject, resourceId);
            configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
        }

        return configuration;
    }

    public int getConfigId() {
        return getConfiguration().getId();
    }

    public String discard() {
        nullify();
        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
    }

    @End
    public String commit() {

        for (RawConfiguration raw : modified.values()) {
            getRaws().put(raw.getPath(), raw);
        }

        getConfiguration().getRawConfigurations().clear();
        getConfiguration().getRawConfigurations().addAll(getRaws().values());
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        getConfigurationManager().updateResourceConfiguration(subject, getResourceId(), getConfiguration());

        nullify();

        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
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

        log.error("setCurrent Called");

        //String u2 = updated.substring(2);
        String original = new String(getCurrent().getContents());
        if (!updated.equals(original)) {
            log.error("original:" + original + ":");
            log.error("updated :" + updated + ":");
            {

                byte[] oBytes = original.getBytes();
                byte[] uBytes = updated.getBytes();

                int max = oBytes.length < uBytes.length ? oBytes.length : uBytes.length;
                for (int i = 0; i < max; ++i) {
                    log.error("original = " + Byte.toString(oBytes[i]) + ": updated = " + Byte.toString(uBytes[i]));
                }
            }

            current = current.deepCopy();
            current.setContents(updated.getBytes());
            //TODO update other values like MD5
            getModified().put(current.getPath(), current);
        }
    }

    public void setCurrentPath(String path) {
        RawConfiguration raw = getModified().get(path);
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

    public TreeMap<String, RawConfiguration> getModified() {
        if (modified == null) {
            modified = new TreeMap<String, RawConfiguration>();
        }
        return modified;
    }

    public boolean isModified(String path) {
        return getModified().keySet().contains(path);
    }

    private ConfigurationManagerLocal getConfigurationManager() {
        if (null == configurationManager) {
            configurationManager = LookupUtil.getConfigurationManager();
        }
        return configurationManager;
    }

    private ConfigurationDefinition getConfigurationDefinition() {
        if (null == configurationDefinition) {

            configurationDefinition = getConfigurationManager().getResourceConfigurationDefinitionForResourceType(
                EnterpriseFacesContextUtility.getSubject(),
                EnterpriseFacesContextUtility.getResource().getResourceType().getId());
        }
        return configurationDefinition;
    }

    private ConfigurationFormat getConfigurationFormat() {
        if (useMock)
            return ConfigurationFormat.STRUCTURED_AND_RAW;

        return getConfigurationDefinition().getConfigurationFormat();
    }

    public boolean isRawSupported() {
        return getConfigurationFormat().isRawSupported();
    }

    public boolean isStructuredSupported() {
        return getConfigurationFormat().isStructuredSupported();
    }
}

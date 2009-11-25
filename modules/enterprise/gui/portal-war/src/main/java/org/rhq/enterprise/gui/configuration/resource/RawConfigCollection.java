package org.rhq.enterprise.gui.configuration.resource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.event.UploadEvent;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.enterprise.gui.configuration.AbstractConfigurationUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

@Name("rawConfigCollection")
@Scope(ScopeType.PAGE)
/**
 * The backing class for all Web based activities for manipulating the set of
 * raw configuration files associated with a resource
 */
public class RawConfigCollection extends AbstractConfigurationUIBean implements Serializable {

    private RawConfigDelegate rawConfigDelegate = null;
    private RawConfigDelegateMap rawConfigDelegateMap;

    /*This is for development, to prevent actually going to the EJBs.
    TODO It should be set to false prior to check in*/
    private static final boolean useMock = false;

    private final Log log = LogFactory.getLog(RawConfigCollection.class);
    private Integer resourceId = null;

    private static final long serialVersionUID = 4837157548556168146L;

    public RawConfigCollection() {
    }

    @End
    public String commit() {

        Subject subject = EnterpriseFacesContextUtility.getSubject();
        LookupUtil.getConfigurationManager().updateResourceConfiguration(subject, getResourceId(),
            getMergedConfiguration());

        nullify();

        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
    }

    private Configuration getMergedConfiguration() {
        for (RawConfiguration raw : getRawConfigDelegate().modified.values()) {
            getRaws().put(raw.getPath(), raw);
            log.error("Just merged in raw path =[" + raw.getPath() + "]");
            log.error("                   file =[" + new String(raw.getContents()) + "]");

        }
        getConfiguration().getRawConfigurations().clear();
        getConfiguration().getRawConfigurations().addAll(getRaws().values());
        return getConfiguration();
    }

    public String discard() {
        nullify();
        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
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

    public int getConfigId() {
        return getConfiguration().getId();
    }

    public Configuration getConfiguration() {
        return getRawConfigDelegate().getConfiguration();
    }

    //TODO Sync this up with the baseclass
    public ConfigurationDefinition getConfigurationDefinition() {
        if (null == getRawConfigDelegate().configurationDefinition) {

            getRawConfigDelegate().configurationDefinition = LookupUtil.getConfigurationManager()
                .getResourceConfigurationDefinitionForResourceType(EnterpriseFacesContextUtility.getSubject(),
                    EnterpriseFacesContextUtility.getResource().getResourceType().getId());
        }
        return getRawConfigDelegate().configurationDefinition;
    }

    private ConfigurationFormat getConfigurationFormat() {
        if (useMock)
            return ConfigurationFormat.STRUCTURED_AND_RAW;
        return getConfigurationDefinition().getConfigurationFormat();
    }

    public RawConfiguration getCurrent() {
        if (null == getRawConfigDelegate().current) {
            Iterator<RawConfiguration> iterator = getRaws().values().iterator();
            if (iterator.hasNext()) {
                getRawConfigDelegate().current = iterator.next();
            } else {
                getRawConfigDelegate().current = new RawConfiguration();
                getRawConfigDelegate().current.setPath("/dev/null");
                getRawConfigDelegate().current.setContents("".getBytes());
            }
        }
        return getRawConfigDelegate().current;
    }

    public String getCurrentContents() {
        return new String(getCurrent().getContents());
    }

    public String getCurrentPath() {
        return getCurrent().getPath();
    }

    private RawConfigDelegateMap getDelegates() {
        return rawConfigDelegateMap;
    }

    public TreeMap<String, RawConfiguration> getModified() {
        if (getRawConfigDelegate().modified == null) {
            getRawConfigDelegate().modified = new TreeMap<String, RawConfiguration>();
        }
        return getRawConfigDelegate().modified;
    }

    public Object[] getPaths() {
        return getRaws().keySet().toArray();
    }

    RawConfigDelegate getRawConfigDelegate() {
        if (null == rawConfigDelegate) {
            rawConfigDelegate = getDelegates().get(getResourceId());
            if (null == rawConfigDelegate) {
                rawConfigDelegate = new RawConfigDelegate(getResourceId());
                getDelegates().put(getResourceId(), rawConfigDelegate);
            }
        }
        return rawConfigDelegate;
    }

    public RawConfigDelegateMap getRawConfigDelegateMap() {
        return rawConfigDelegateMap;
    }

    public TreeMap<String, RawConfiguration> getRaws() {
        return getRawConfigDelegate().getRaws();
    }

    /**
    *      
    * @return the id associated with the resource.  
    * The value Cached in order to be available on the upload page, 
    * where seeing the resource id conflicts with the rich upload tag.
    */
    public int getResourceId() {
        if (resourceId == null) {
            resourceId = EnterpriseFacesContextUtility.getResource().getId();
        }
        return resourceId;
    }

    /**
     * Hack Alert.  This bean needs to be initialized on one of the pages that has id or resourceId set
     * In order to capture the resource.  It will then Keep track of that particular resources until
     * commit is called.  Ideally, this should be a conversation scoped bean, but that has other issues
     * 
     */
    @Create
    public void init() {

    }

    public boolean isModified(String path) {
        return getModified().keySet().contains(path);
    }

    public boolean isRawSupported() {
        return getConfigurationFormat().isRawSupported();
    }

    public boolean isStructuredSupported() {
        return getConfigurationFormat().isStructuredSupported();
    }

    void nullify() {
        getDelegates().remove(getResourceId());
        setRawConfigDelegate(null);
    }

    /**
     * Indicates which of the raw configuration files is currently selected.
     * @param s
     */
    public void select(String s) {
        getRawConfigDelegate().selectedPath = s;
        setCurrentPath(getRawConfigDelegate().selectedPath);
    }

    public void setCurrentContents(String updated) {

        String original = new String(getCurrent().getContents());
        if (!updated.equals(original)) {
            getRawConfigDelegate().current = getRawConfigDelegate().current.deepCopy(false);
            getRawConfigDelegate().current.setContents(updated.getBytes());
            //TODO update other values like MD5
            getModified().put(getRawConfigDelegate().current.getPath(), getRawConfigDelegate().current);
        }
    }

    public void setCurrentPath(String path) {
        RawConfiguration raw = getModified().get(path);
        if (null == raw) {
            raw = getRaws().get(path);
        }
        if (null != raw) {
            getRawConfigDelegate().current = raw;
        }
    }

    void setDelegates(RawConfigDelegateMap delegates) {
        this.rawConfigDelegateMap = delegates;
    }

    public void setModified(RawConfiguration raw) {
        getRawConfigDelegate().modified.put(raw.getPath(), raw);
    }

    void setRawConfigDelegate(RawConfigDelegate rawConfigDelegate) {
        this.rawConfigDelegate = rawConfigDelegate;
    }

    @In(create = true, required = true)
    public void setRawConfigDelegateMap(RawConfigDelegateMap rawConfigDelegateMap) {
        this.rawConfigDelegateMap = rawConfigDelegateMap;
    }

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "save" icon from the full screen page
     */
    public String update() {
        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "action" value 
     */
    public String upload() {
        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    public String switchToraw() {
        log.error("switch2raw called");
        dumpProperties(getConfiguration(), log);
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getMergedConfiguration(), true);
        log.error("switch2raw post merge");
        dumpProperties(getConfiguration(), log);

        getRawConfigDelegate().setConfiguration(configuration);
        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            getRawConfigDelegate().getRaws().put(raw.getPath(), raw);
        }
        getRawConfigDelegate().current = null;
        getRawConfigDelegate().setConfiguration(configuration);
        getRawConfigDelegateMap().put(getResourceId(), getRawConfigDelegate());

        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    void dumpProperties(Configuration conf, Log log) {
        for (String key : conf.getAllProperties().keySet()) {
            log.error("property=" + conf.getAllProperties().get(key));
        }
    }

    public String switchTostructured() {
        log.error("switch2structured called");

        dumpProperties(getConfiguration(), log);
        Configuration configuration = LookupUtil.getConfigurationManager().translateResourceConfiguration(
            EnterpriseFacesContextUtility.getSubject(), getResourceId(), getMergedConfiguration(), false);
        log.error("switch2structured post merge");

        dumpProperties(configuration, log);

        for (Property property : configuration.getAllProperties().values()) {
            property.setConfiguration(configuration);
        }

        for (RawConfiguration raw : configuration.getRawConfigurations()) {
            getRawConfigDelegate().getRaws().put(raw.getPath(), raw);
            getRawConfigDelegate().setConfiguration(configuration);
        }
        getRawConfigDelegate().current = null;
        getRawConfigDelegate().setConfiguration(configuration);

        return "/rhq/resource/configuration/edit.xhtml?currentResourceId=" + getResourceId();
    }

    @Override
    protected int getConfigurationDefinitionKey() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected int getConfigurationKey() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected Configuration lookupConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ConfigurationDefinition lookupConfigurationDefinition() {
        // TODO Auto-generated method stub
        return null;
    }

}

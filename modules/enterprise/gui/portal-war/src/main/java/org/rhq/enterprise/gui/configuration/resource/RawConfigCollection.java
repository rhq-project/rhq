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
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
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

@Name("rawConfigCollection")
@Scope(ScopeType.PAGE)
/**
 * The backing class for all Web based activities for manipulating the set of
 * raw configuration files associated with a resource
 */
public class RawConfigCollection implements Serializable {

    private RawConfigDelegate rawConfigDelegate = null;
    private RawConfigDelegateMap rawConfigDelegateMap;

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
    private Integer resourceId = null;

    private static final long serialVersionUID = 4837157548556168146L;

    public RawConfigCollection() {
    }

    @End
    public String commit() {

        for (RawConfiguration raw : getRawConfigDelegate().modified.values()) {
            getRaws().put(raw.getPath(), raw);
        }

        getConfiguration().getRawConfigurations().clear();
        getConfiguration().getRawConfigurations().addAll(getRaws().values());
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        getConfigurationManager().updateResourceConfiguration(subject, getResourceId(), getConfiguration());

        nullify();

        return "/rhq/resource/configuration/view.xhtml?id=" + getResourceId();
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
        if (null == configuration) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            AbstractResourceConfigurationUpdate configurationUpdate = this.getConfigurationManager()
                .getLatestResourceConfigurationUpdate(subject, getRawConfigDelegate().resourceId);
            configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
        }

        return configuration;
    }

    private ConfigurationDefinition getConfigurationDefinition() {
        if (null == getRawConfigDelegate().configurationDefinition) {

            getRawConfigDelegate().configurationDefinition = getConfigurationManager()
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

    private ConfigurationManagerLocal getConfigurationManager() {
        if (null == configurationManager) {
            configurationManager = LookupUtil.getConfigurationManager();
        }
        return configurationManager;
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
        if (null == getRawConfigDelegate().raws) {
            getRawConfigDelegate().raws = new TreeMap<String, RawConfiguration>();
            if (useMock) {
                populateRawsMock();
            } else {
                populateRaws();
            }
        }
        return getRawConfigDelegate().raws;
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

    private void populateRaws() {
        Collection<RawConfiguration> rawConfigurations = getConfigurationManager()
            .findRawConfigurationsByConfigurationId(getConfigId());
        for (RawConfiguration raw : rawConfigurations) {
            getRawConfigDelegate().raws.put(raw.getPath(), raw);
        }
    }

    private void populateRawsMock() {
        String[] files = { "/etc/mock/file1", "/etc/mock/file2", "/etc/mock/file3", "/etc/mock/me/will/you",
            "/etc/mock/turtle/soup", "/etc/mock/mysmock/iclean/yourclock" };

        String[] filesContents = {
            "GALLIA est omnis divisa in partes tres, quarum unam incolunt Belgae, \naliam Aquitani, tertiam qui ipsorum lingua Celtae, \n"
                + "nostra Galli appellantur. \nHi omnes lingua, institutis, legibus inter se differunt. \n",
            "I've seen all good people \n turn their heads each day \n So Satisfied\n I'm on my way",
            "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor \nincididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis\n nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu\n fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in \nculpa qui officia deserunt mollit anim id est laborum.",
            "It was on a dreary night of November that I beheld the accomplishment of my toils. With an anxiety that "
                + "almost amounted to agony, I collected the instruments of life around me, that I might infuse a spark of being "
                + "into the lifeless thing that lay at my feet. It was already one in the morning; the rain pattered dismally "
                + "against the panes, and my candle was nearly burnt out, when, by the glimmer of the half-extinguished light, "
                + "I saw the dull yellow eye of the creature open; it breathed hard, and a convulsive motion agitated its limbs.\n\n"
                + "How can I describe my emotions at this catastrophe, or how delineate the wretch whom with such infinite pains and "
                + "care I had endeavoured to form? His limbs were in proportion, and I had selected his features as beautiful. "
                + "Beautiful! Great God! His yellow skin scarcely covered the work of muscles and arteries beneath; his hair was "
                + "of a lustrous black, and flowing; his teeth of a pearly whiteness; but these luxuriances only formed a more "
                + "horrid contrast with his watery eyes, that seemed almost of the same colour as the dun-white sockets in which "
                + "they were set, his shrivelled complexion and straight black lips. ",
            "\"The time has come,\" the Walrus said,\n" + "To talk of many things:\n"
                + "Of shoes--and ships--and sealing-wax--\n" + "Of cabbages--and kings--\n"
                + "And why the sea is boiling hot--\n" + "And whether pigs have wings.",
            "My grandfather's clock\n" + "Was too large for the shelf,\n" + "So it stood ninety years on the floor;\n"
                + "It was taller by half\n" + "Than the old man himself,\n"
                + "Though it weighed not a pennyweight more.\n" + "It was bought on the morn\n"
                + "Of the day that he was born,\n" + "It was always his treasure and pride;" + "But it stopped short\n"
                + "Never to go again,\n" + "When the old man died." };
        int i = 0;
        for (String file : files) {
            RawConfiguration raw = new RawConfiguration();
            raw.setPath(file);
            raw.setContents((filesContents[i++]).getBytes());
            getRawConfigDelegate().raws.put(file, raw);
        }

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
            getRawConfigDelegate().current = getRawConfigDelegate().current.deepCopy();
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
        log.error("update called");

        return "/rhq/resource/configuration/edit-raw.xhtml?currentResourceId=" + getResourceId();
    }

    /**
     * This is a no-op, since the upload work was done by upload file
     * But is kept as a target for the "action" value 
     */
    public void upload() {
    }
}

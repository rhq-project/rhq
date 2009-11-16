package org.rhq.enterprise.gui.configuration.resource;

import java.util.Iterator;
import java.util.TreeMap;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

@Name("RawConfigViewer")
@Scope(ScopeType.SESSION)
public class RawConfigViewer {

    RawConfigDelegate rawConfigDelegate;

    int getResourceId() {
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        return resourceId;
    }

    public RawConfigDelegate getRawConfigDelegate() {
        if (null == rawConfigDelegate || (getResourceId() != rawConfigDelegate.resourceId)) {
            rawConfigDelegate = new RawConfigDelegate(getResourceId());
        }
        return rawConfigDelegate;
    }

    public TreeMap<String, RawConfiguration> getRaws() {
        return getRawConfigDelegate().getRaws();
    }

    public void setCurrentPath(String path) {
        RawConfiguration raw = getRaws().get(path);
        if (null != raw) {
            getRawConfigDelegate().current = raw;
        }
    }

    public String getCurrentContents() {
        return new String(getCurrent().getContents());
    }

    public Object[] getPaths() {
        return getRaws().keySet().toArray();
    }

    public void select(String s) {
        getRawConfigDelegate().selectedPath = s;
        setCurrentPath(getRawConfigDelegate().selectedPath);
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

}

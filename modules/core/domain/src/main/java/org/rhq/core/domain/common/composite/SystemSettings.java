package org.rhq.core.domain.common.composite;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * This class represents the system settings of the RHQ server. Whenever a new property is added, 
 * it must be reflected by a property in this map (and therefore defined in the {@link SystemProperty} enum). 
 *
 * @author John Sanda
 * @author Lukas Krejci
 */
public class SystemSettings extends HashMap<SystemProperty, String> implements Serializable {

    private static final long serialVersionUID = 2L;

    private Map<String, String> driftPlugins = new HashMap<String, String>();

    public static SystemSettings fromMap(Map<String, String> properties) {
        SystemSettings ret = new SystemSettings();
        
        for(Map.Entry<String, String> e : properties.entrySet()) {
            SystemProperty p = SystemProperty.getByInternalName(e.getKey());
            if (p == null) {
                throw new IllegalArgumentException("'" + e.getKey() + "' is not a system property.");
            }
            
            ret.put(p, e.getValue());
        }
        
        return ret;
    }
    
    public SystemSettings() {
    }
    
    /**
     * A copy-constructor, because GWT doesn't support Cloneable.
     * (Seriously, Google?)
     * 
     * @see <a href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1843">http://code.google.com/p/google-web-toolkit/issues/detail?id=1843</a>
     * @param original
     */
    public SystemSettings(SystemSettings original) {
        this.putAll(original);
        
        if (original.driftPlugins != null) {
            driftPlugins.putAll(original.driftPlugins);
        }
    }
    
    @Override
    public String put(SystemProperty key, String value) {
        if (key.validateValue(value)) {
            return super.put(key, value);
        } else {
            throw new IllegalArgumentException("Value [" + value + "] is not valid for property '" + key.getInternalName() + "'.");
        }
    }
    
    @Override
    public void putAll(Map<? extends SystemProperty, ? extends String> m) {
        for(Map.Entry<? extends SystemProperty, ? extends String> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }
    
    public Map<String, String> getDriftPlugins() {
        return driftPlugins;
    }

    public void setDriftPlugins(Map<String, String> driftPlugins) {
        this.driftPlugins = driftPlugins;
    }

    public Configuration toConfiguration() {
        Configuration ret = new Configuration();
        for(Map.Entry<SystemProperty, String> e : entrySet()) {
            ret.put(new PropertySimple(e.getKey().getInternalName(), e.getValue()));
        }
        
        return ret;
    }
    
    public void applyConfiguration(Configuration configuration) {
        for(PropertySimple prop : configuration.getSimpleProperties().values()) {
            SystemProperty systemProp = SystemProperty.getByInternalName(prop.getName());
            
            if (systemProp != null) {
                String value = prop.getStringValue();
                put(systemProp, value);
            }
        }
    }      
    
    public Map<String, String> toMap() {
        HashMap<String, String> ret = new HashMap<String, String>(size());
        
        for(Map.Entry<SystemProperty, String> e : entrySet()) {
            ret.put(e.getKey().getInternalName(), e.getValue());
        }
        
        return ret;
    }    
}

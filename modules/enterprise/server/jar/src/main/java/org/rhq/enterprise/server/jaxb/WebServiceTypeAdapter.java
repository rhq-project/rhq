package org.rhq.enterprise.server.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.configuration.Configuration;

/** See the javadoc for the XmlAdapter itself for a more complete explanation, but simply 
 *  put the adapter maps types that JAXB has difficulty serializing(Ex. Map<int,Configuration>) 
 *  to java types that JAXB can actually serialize.  From a JAXB perspective think of the 
 *  problematic types as opaque to JAXB and the map to type in a marshal as the serializable
 *  type.
 * 
 * @author Simeon Pinder
 *
 */
public class WebServiceTypeAdapter extends XmlAdapter<Configuration[], Map<Integer, Configuration>> {

    @Override
    public Configuration[] marshal(Map<Integer, Configuration> opaque) throws Exception {
        Configuration[] bag = null;
        if (opaque != null) {
            bag = opaque.values().toArray(new Configuration[opaque.size()]);
        } else {
            bag = new Configuration[0];
        }
        return bag;
    }

    @Override
    public Map<Integer, Configuration> unmarshal(Configuration[] serializable) throws Exception {
        Map<Integer, Configuration> map = new HashMap<Integer, Configuration>();
        int i = 0;
        if (serializable != null) {
            for (Configuration c : serializable) {
                map.put(Integer.valueOf(i++), c);
            }
        }
        return map;
    }
}

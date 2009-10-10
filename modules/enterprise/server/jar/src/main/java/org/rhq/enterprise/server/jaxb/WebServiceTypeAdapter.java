package org.rhq.enterprise.server.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebResult;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.server.system.ServerVersion;

/** See the javadoc for the XmlAdapter itself for a more complete explanation, but simply 
 *  put the adapter maps types that JAXB has difficulty serializing(Ex. Map<int,Configuration>) 
 *  to java types that JAXB can actually serialize.  From a JAXB perspective think of the 
 *  problematic types as opaque to JAXB and the map to type in a marshal as the serializable
 *  type.
 * 
 * @author Simeon Pinder
 *
 */
@XmlType(namespace = ServerVersion.namespace)
public class WebServiceTypeAdapter extends XmlAdapter<Object[], Map<Integer, Configuration>> {

    @WebResult(targetNamespace = ServerVersion.namespace)
    public Object[] marshal(Map<Integer, Configuration> opaque) throws Exception {
        Object[] bag = null;
        if (opaque != null) {
            int i = 0;
            bag = new Object[2 * opaque.size()];
            for (Map.Entry<Integer, Configuration> mapEntry : opaque.entrySet()) {
                bag[i++] = mapEntry.getKey();
                bag[i++] = mapEntry.getValue();
            }
        } else {
            bag = new Object[0];
        }
        return bag;
    }

    @WebResult(targetNamespace = ServerVersion.namespace)
    public Map<Integer, Configuration> unmarshal(Object[] marshallable) throws Exception {
        Map<Integer, Configuration> map = new HashMap<Integer, Configuration>();
        if (marshallable != null) {
            for (int i = 0; i < marshallable.length; i += 2) {
                map.put((Integer) marshallable[i], (Configuration) marshallable[i + 1]);
            }
        }
        return map;
    }
}

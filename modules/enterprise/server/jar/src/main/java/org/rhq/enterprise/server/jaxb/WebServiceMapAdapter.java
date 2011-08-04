package org.rhq.enterprise.server.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebResult;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.enterprise.server.system.ServerVersion;

/** See the javadoc for the XmlAdapter itself for a more complete explanation, but simply 
 *  put the adapter maps types that JAXB has difficulty serializing(Ex. Map<Integer,String>) 
 *  to java types that JAXB can actually serialize.  From a JAXB perspective think of the 
 *  problematic types as opaque to JAXB and the map to type in a marshal as the serializable
 *  type.
 * 
 * @author Simeon Pinder
 * @author Filip Drabek
 *
 */
@XmlType(namespace = ServerVersion.namespace)
public class WebServiceMapAdapter extends XmlAdapter<Object[], Map<Integer, String>> {

    @WebResult(targetNamespace = ServerVersion.namespace)
    public Object[] marshal(Map<Integer, String> opaque) throws Exception {
        Object[] bag = null;
        if (opaque != null) {
            int i = 0;
            bag = new Object[2 * opaque.size()];
            for (Map.Entry<Integer, String> mapEntry : opaque.entrySet()) {
                bag[i++] = mapEntry.getKey();
                bag[i++] = mapEntry.getValue();
            }
        } else {
            bag = new Object[0];
        }
        return bag;
    }

    @WebResult(targetNamespace = ServerVersion.namespace)
    public Map<Integer, String> unmarshal(Object[] marshallable) throws Exception {
        Map<Integer, String> map = new HashMap<Integer, String>();
        if (marshallable != null) {
            for (int i = 0; i < marshallable.length; i += 2) {
                map.put((Integer) marshallable[i], (String) marshallable[i + 1]);
            }
        }
        return map;
    }
}

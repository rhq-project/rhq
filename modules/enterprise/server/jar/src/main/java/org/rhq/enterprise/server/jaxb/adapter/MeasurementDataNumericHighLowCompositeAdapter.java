package org.rhq.enterprise.server.jaxb.adapter;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebResult;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
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
public class MeasurementDataNumericHighLowCompositeAdapter
    extends
    XmlAdapter<ArrayList<ArrayList<MeasurementDataNumericHighLowComposite>>, List<List<MeasurementDataNumericHighLowComposite>>> {

    public ArrayList<ArrayList<MeasurementDataNumericHighLowComposite>> marshal(
        List<List<MeasurementDataNumericHighLowComposite>> opaque) throws Exception {
        ArrayList<ArrayList<MeasurementDataNumericHighLowComposite>> converted = new ArrayList<ArrayList<MeasurementDataNumericHighLowComposite>>();
        if (opaque != null) {
            for (List<MeasurementDataNumericHighLowComposite> li : opaque) {
                converted.add((ArrayList<MeasurementDataNumericHighLowComposite>) li);
            }
        }
        return converted;
    }

    @WebResult(targetNamespace = ServerVersion.namespace)
    public List<List<MeasurementDataNumericHighLowComposite>> unmarshal(
        ArrayList<ArrayList<MeasurementDataNumericHighLowComposite>> marshallable) throws Exception {
        return new ArrayList<List<MeasurementDataNumericHighLowComposite>>(marshallable);
    }

}

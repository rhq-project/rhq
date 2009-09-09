/*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */

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

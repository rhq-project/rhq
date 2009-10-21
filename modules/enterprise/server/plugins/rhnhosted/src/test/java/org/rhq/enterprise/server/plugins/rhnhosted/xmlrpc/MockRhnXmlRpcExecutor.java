/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNameType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnProductNamesType;
import org.rhq.enterprise.server.plugins.rhnhosted.xml.RhnSatelliteType;

/**
 * @author mmccune
 *
 */
public class MockRhnXmlRpcExecutor implements XmlRpcExecutor {

    /**
     * Constructor
     * @param client to ignore
     */
    public MockRhnXmlRpcExecutor(XmlRpcClient client) {

    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.XmlRpcExecutor#execute(java.lang.String, java.lang.Object[])
     */
    @Override
    public Object execute(String methodName, Object[] params) throws XmlRpcException {
        System.out.println("MethodName: " + methodName);
        for (Object object : params) {
            System.out.println("parm: " + object);
        }
        if (methodName.equals("authentication.check")) {
            String systemid = (String) params[0];
            if (systemid.contains("<name>system_id</name>")) {
                return new Integer(1);
            } else {
                return new Integer(0);
            }
        }
        if (methodName.equals("dump.product_names")) {

            // getRhnProductNames().getRhnProductName();
            RhnProductNameType type = new RhnProductNameType();
            type.setLabel("some-prod-label");
            type.setName("some product name");
            List<RhnProductNameType> rhnProductName = new LinkedList<RhnProductNameType>();
            rhnProductName.add(type);

            RhnProductNamesType prodNamesType = new RhnProductNamesType();
            try {
                Field privateListField = RhnProductNamesType.class.getDeclaredField("rhnProductName");
                privateListField.setAccessible(true);
                privateListField.set(prodNamesType, rhnProductName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            RhnSatelliteType sattype = new RhnSatelliteType();
            sattype.setRhnProductNames(prodNamesType);

            JAXBElement<RhnSatelliteType> element = new JAXBElement<RhnSatelliteType>(new QName(""),
                RhnSatelliteType.class, sattype);
            element.setValue(sattype);
            return element;
        }
        return null;
    }
}

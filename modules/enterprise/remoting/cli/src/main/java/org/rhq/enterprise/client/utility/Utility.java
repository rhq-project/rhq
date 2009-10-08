/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.client.utility;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceClient;

public class Utility {

    /**Dynamically builds the WSDL URL to connect to a remote server.
     *
     * @param remote class correctly annotated with Webservice reference.
     * @return valid URL
     * @throws MalformedURLException
     */
    public static URL generateRhqRemoteWebserviceURL(Class remote, String host, int port, boolean useHttps)
        throws MalformedURLException {

        URL wsdlLocation = null;
        //TODO: what to do about exceptions/messaging? throw illegalArgs?
        //insert checks for host, port
        if ((host == null) || (host.trim().length() == 0) || (port <= 0)) {
            return wsdlLocation;
        }

        //check for reference for right annotations
        if ((remote != null) && remote.isAnnotationPresent(WebServiceClient.class)) {
            String beanName = remote.getSimpleName();
            String protocol = "https://";
            if (!useHttps) {
                protocol = "http://";
            }
            wsdlLocation = new URL(protocol + host + ":" + port + "/rhq-rhq-enterprise-server-ejb3/"
                + beanName.substring(0, beanName.length() - "Service".length()) + "?wsdl");
        }
        return wsdlLocation;

    }

    public static QName generateRhqRemoteWebserviceQName(Class remote) {

        QName generated = null;
        //check for reference with right annotation
        if ((remote != null) && (remote.isAnnotationPresent(WebServiceClient.class))) {
            String annotatedQnameValue = "";
            Annotation annot = remote.getAnnotation(WebServiceClient.class);
            WebServiceClient annotated = (WebServiceClient) annot;
            annotatedQnameValue = annotated.targetNamespace();
            String beanName = remote.getSimpleName();

            generated = new QName(annotatedQnameValue, beanName);
        }
        return generated;

    }

}

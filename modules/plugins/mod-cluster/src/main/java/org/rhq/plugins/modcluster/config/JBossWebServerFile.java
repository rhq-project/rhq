/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.modcluster.config;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @author Stefan Negrea
 *
 */
public class JBossWebServerFile extends AbstractConfigurationFile {

    public JBossWebServerFile(String fileName) throws ParserConfigurationException, SAXException, IOException {
        super(fileName);
        // TODO Auto-generated constructor stub
    }

    @Override
    void setPropertyValue(String propertyName, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    String getPropertyValue(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    void saveConfigurationFile() throws Exception {
        // TODO Auto-generated method stub

    }

}

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

package org.rhq.enterprise.server.plugins.rhnhosted.certificate;

import org.jdom.Element;
import org.jdom.JDOMException;

/**
 * @author pkilambi
 *
 */
/**
 * An interface to describe how a field is to be extracted from the DOM
 * representation of the XML rhq certificate
 */
interface FieldExtractor {

    void extract(Certificate target, Element field) throws JDOMException;

    boolean isRequired();

    String getFieldName();

}

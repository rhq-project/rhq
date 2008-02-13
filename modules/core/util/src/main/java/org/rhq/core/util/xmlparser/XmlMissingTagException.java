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
package org.rhq.core.util.xmlparser;

import org.jdom.Element;

/**
 * An exception which is thrown when a tag does not have a required child tag. This exception can be thrown when a tag
 * is required and is missing, or has a ONE_OR_MORE and is missing.
 */
public class XmlMissingTagException extends XmlTagException {
    XmlMissingTagException(Element e, String reqTag) {
        super("Tag <" + e.getName() + "> requires child tag '" + reqTag + "'");
    }
}
/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.core.pluginapi.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An XML entity resolver that skips resolution of any entities with system IDs that match a specified regular
 * expression. The {@link #getDtdAndXsdSkippingInstance()} convenience method can be used to return an instance
 * that skips resolution of DTDs and XML schemas.
 *
 * @author Ian Springer
 */
public class SelectiveSkippingEntityResolver implements EntityResolver {

    private static final Log LOG = LogFactory.getLog(SelectiveSkippingEntityResolver.class);

    private static SelectiveSkippingEntityResolver dtdAndXsdSkippingInstance;

    private Pattern systemIdsToSkipPattern;

    public SelectiveSkippingEntityResolver(String systemIdsToSkipRegex) {
        if (systemIdsToSkipRegex != null) {
            this.systemIdsToSkipPattern = Pattern.compile(systemIdsToSkipRegex);
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if ((this.systemIdsToSkipPattern != null) && (systemId != null) &&
            this.systemIdsToSkipPattern.matcher(systemId).find()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping resolution of entity [publicID=" + publicId + ", systemID=" + systemId + "]...");
            }
            // Return an empty input source to the parser, so it will think the entity was resolved.
            StringReader emptyStringReader = new StringReader("");
            return new InputSource(emptyStringReader);
        } else {
            // Return null to tell the parser to use its default behavior of opening a regular URI connection to the
            // system ID.
            return null;
        }
    }

    public static SelectiveSkippingEntityResolver getDtdAndXsdSkippingInstance() {
        if (dtdAndXsdSkippingInstance == null) {
            dtdAndXsdSkippingInstance = new SelectiveSkippingEntityResolver("\\.(dtd|xsd)$");
        }
        return dtdAndXsdSkippingInstance;
    }

}

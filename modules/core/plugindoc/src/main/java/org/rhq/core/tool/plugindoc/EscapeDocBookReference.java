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
package org.rhq.core.tool.plugindoc;

import org.apache.velocity.app.event.implement.EscapeXmlReference;

/**
 * A velocity {@link org.apache.velocity.app.event.ReferenceInsertionEventHandler} that escapes DocBook special
 * characters.  It was reported by DocBook maintainers that escaping apostrophe characters to "&apos;" does not
 * render correct with the used DocBook version.  So, this class has same semantics as {@link EscapeXmlReference}
 * except that it does not escape or unescape apostrophe characters.  Configure this handler as follows:
 * <code>
 * velocityConfig.setProperty("eventhandler.referenceinsertion.class", EscapeDocBookReference.class.getName());
 * </code>
 *
 * @author Joseph Marques
 */
public class EscapeDocBookReference extends EscapeXmlReference {

    @Override
    protected String escape(Object text) {
        String results = super.escape(text);
        results = results.replaceAll("&apos;", "'");
        return results;
    }

}

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

import org.rhq.enterprise.server.plugins.rhnhosted.certificate.XmlTag;
/**
 * @author pkilambi
 *
 */
/**
 * The entitlements for a channel family, consisting of the family name and a
 * quantity.
 * 
 */
public class ChannelFamilyDescriptor implements Comparable {

    private String family;
    private String quantity;

    ChannelFamilyDescriptor(String family0, String quantity0) {
        family = family0;
        quantity = quantity0;
    }

    /**
     * Return the name of this channel family
     * @return the name of this channel family
     */
    public String getFamily() {
        return family;
    }

    /**
     * Return the quantity for this family
     * @return the quantity for this family
     */
    public String getQuantity() {
        return quantity;
    }

    String asChecksumString() {
        return "channel-families-family-" + getFamily() + "-quantity-" + getQuantity();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Object obj) {
        ChannelFamilyDescriptor other = (ChannelFamilyDescriptor) obj;
        // The sort order for families is kinda odd; this replicates
        // exactly the way the Perl code sorts the fields so that
        // signature checking on the result is possible across Perl and Java
        return asSortKey().compareTo(other.asSortKey());
    }

    private String asSortKey() {
        return getQuantity() + "familyquantity" + getFamily();
    }

    String asXmlString() {
        XmlTag tag = new XmlTag("rhn-cert-field", false);
        tag.setAttribute("name", "channel-families");
        tag.setAttribute("quantity", getQuantity());
        tag.setAttribute("family", getFamily());
        return tag.render();
    }
}

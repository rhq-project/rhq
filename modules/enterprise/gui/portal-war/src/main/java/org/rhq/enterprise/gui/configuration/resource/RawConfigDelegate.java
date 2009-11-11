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

package org.rhq.enterprise.gui.configuration.resource;

import java.io.Serializable;
import java.util.TreeMap;

import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

public class RawConfigDelegate implements Serializable {

    public RawConfigDelegate(int resourceId) {
        super();
        this.resourceId = resourceId;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -9058700205958371765L;
    int resourceId = 0;
    String selectedPath;
    TreeMap<String, RawConfiguration> raws;
    TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    RawConfiguration current = null;
    ConfigurationDefinition configurationDefinition = null;

    void populateRawsMock(RawConfigCollection rawConfigCollection) {
        String[] files = { "/etc/mock/file1", "/etc/mock/file2", "/etc/mock/file3", "/etc/mock/me/will/you",
            "/etc/mock/turtle/soup", "/etc/mock/mysmock/iclean/yourclock" };

        String[] filesContents = {
            "GALLIA est omnis divisa in partes tres, quarum unam incolunt Belgae, \naliam Aquitani, tertiam qui ipsorum lingua Celtae, \n"
                + "nostra Galli appellantur. \nHi omnes lingua, institutis, legibus inter se differunt. \n",
            "I've seen all good people \n turn their heads each day \n So Satisfied\n I'm on my way",
            "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor \nincididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis\n nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\n Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu\n fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in \nculpa qui officia deserunt mollit anim id est laborum.",
            "It was on a dreary night of November that I beheld the accomplishment of my toils. With an anxiety that "
                + "almost amounted to agony, I collected the instruments of life around me, that I might infuse a spark of being "
                + "into the lifeless thing that lay at my feet. It was already one in the morning; the rain pattered dismally "
                + "against the panes, and my candle was nearly burnt out, when, by the glimmer of the half-extinguished light, "
                + "I saw the dull yellow eye of the creature open; it breathed hard, and a convulsive motion agitated its limbs.\n\n"
                + "How can I describe my emotions at this catastrophe, or how delineate the wretch whom with such infinite pains and "
                + "care I had endeavoured to form? His limbs were in proportion, and I had selected his features as beautiful. "
                + "Beautiful! Great God! His yellow skin scarcely covered the work of muscles and arteries beneath; his hair was "
                + "of a lustrous black, and flowing; his teeth of a pearly whiteness; but these luxuriances only formed a more "
                + "horrid contrast with his watery eyes, that seemed almost of the same colour as the dun-white sockets in which "
                + "they were set, his shrivelled complexion and straight black lips. ",
            "\"The time has come,\" the Walrus said,\n" + "To talk of many things:\n"
                + "Of shoes--and ships--and sealing-wax--\n" + "Of cabbages--and kings--\n"
                + "And why the sea is boiling hot--\n" + "And whether pigs have wings.",
            "My grandfather's clock\n" + "Was too large for the shelf,\n" + "So it stood ninety years on the floor;\n"
                + "It was taller by half\n" + "Than the old man himself,\n"
                + "Though it weighed not a pennyweight more.\n" + "It was bought on the morn\n"
                + "Of the day that he was born,\n" + "It was always his treasure and pride;" + "But it stopped short\n"
                + "Never to go again,\n" + "When the old man died." };
        int i = 0;
        for (String file : files) {
            RawConfiguration raw = new RawConfiguration();
            raw.setPath(file);
            raw.setContents((filesContents[i++]).getBytes());
            rawConfigCollection.getRawConfigDelegate().raws.put(file, raw);
        }

    }

}
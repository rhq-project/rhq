/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.content;

import java.io.Serializable;

/**
 * This class describes the qualities of a package version format
 * and is used by the UI to provide "next version" hints to the user
 * as well as for checking the version string format validity. 
 * <p>
 * Note that this is going to be used in UI and therefore the regexes
 * need to be written using the Javascript regex synax, *NOT* the Java one.
 * 
 * @author Lukas Krejci
 */
public class PackageVersionFormatDescription implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String fullFormatRegex;
    private String osgiVersionExtractionRegex;
    private int osgiVersionGroupIndex;
    private String textualDesctiption;
    
    //GWT mandates a no-arg constructor, even if just private
    protected PackageVersionFormatDescription() {
        
    }

    /**
     * 
     * @param fullFormatRegex the regex the user supplied version must match 
     * @param osgiVersionExtractionRegex a regex to extract the osgi version string from the full version string.
     * The regex must match the whole version string and have a group enclosing the osgi version string within it.
     * @param osgiVersionGroupIndex the index of the group in the osgi version regex that contains the actual osgi version string
     * @param textualDescription the textual description of format
     */
    public PackageVersionFormatDescription(String fullFormatRegex, String osgiVersionExtractionRegex, int osgiVersionGroupIndex, String textualDescription) {
        super();
        this.fullFormatRegex = fullFormatRegex;
        this.osgiVersionExtractionRegex = osgiVersionExtractionRegex;
        this.osgiVersionGroupIndex = osgiVersionGroupIndex;
        this.textualDesctiption = textualDescription;
    }

    /**
     * @return the fullFormatRegex
     */
    public String getFullFormatRegex() {
        return fullFormatRegex;
    }

    /**
     * @return the osgiVersionExtractionRegex
     */
    public String getOsgiVersionExtractionRegex() {
        return osgiVersionExtractionRegex;
    }
    
    /**
     * @return the osgiVersionGroupIndex
     */
    public int getOsgiVersionGroupIndex() {
        return osgiVersionGroupIndex;
    }
    
    /**
     * @return the textualDesctiption
     */
    public String getTextualDesctiption() {
        return textualDesctiption;
    }
}

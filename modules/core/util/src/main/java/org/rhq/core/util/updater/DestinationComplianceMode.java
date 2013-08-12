/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.core.util.updater;

/**
 * Lists the compliance modes of the bundle deployment destinations. See the comments at the individual enum elements
 * to see what they mean.
 *
 * @author Lukas Krejci
 * @since 4.9
 */
public enum DestinationComplianceMode {
    //NOTE: the below violation of our coding guidelines is because of ANT's requirement for direct usage of enum's
    //constant names in the build files. Because this enum is used generally in all bundle handlers we need to ensure
    //it works for all of them. For readability reasons in ANT bundle recipes, I opted for breaking our guidelines here
    //instead of creating some kind of 'bridge' ant-specific enum.

    /**
     * The full compliance means that the deployment destination is completely wiped before the bundle contents are
     * deployed into it. In another words the destination contains no other files than those contained in the bundle
     * and
     * is therefore in full compliance with the bundle.
     */
    full,

    /**
     * This compliance mode makes sure that files and directories that are NOT contained in the bundle are kept in the
     * destination directory. However the contents of files <b>and directories</b> that ARE present in the bundle are
     * made completely compliant with the bundle.
     */
    filesAndDirectories

    //NOTE: the below two modes are going to be supported in the future, but NOT as of RHQ 4.9.0 */

    /**
     * This compliance mode means that the root directory of the deployment will only contain files and directories from
     * the bundle. The content of the directories is not required to be compliant with the bundle - i.e. the directories
     * and files "under" some directory, that already existed in the deployment, are kept.
     */
    //, rootDirectoryAndFiles

    /**
     * This compliance mode means that all files from a bundle is copied into the deployment (preserving directory
     * structure) (i.e. such files are compliant with the bundle). All other contents of the deployment directory is
     * kept intact (i.e. this is the RPM-like behavior).
     */
    //, files

    ;

    /**
     * This is the default compliance mode to be used in the legacy bundle recipes which do not explicitly set neither
     * the compliance nor the legacy {@code manageRootDir} attribute.
     */
    public static final DestinationComplianceMode BACKWARDS_COMPATIBLE_DEFAULT = full;

    /**
     * Use this method to get either the supplied compliance mode or the {@link #BACKWARDS_COMPATIBLE_DEFAULT default}
     * compliance.
     * <p/>
     * Only use this method if you need to handle the legacy recipes.
     *
     * @param compliance the compliance to return or null if not known
     *
     * @return the supplied {@code compliance} or the default compliance mode, never null.
     */
    public static DestinationComplianceMode instanceOrDefault(DestinationComplianceMode compliance) {
        return compliance == null ? BACKWARDS_COMPATIBLE_DEFAULT : compliance;
    }
}

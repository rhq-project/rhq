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

package org.rhq.plugins.augeas;

import java.util.ArrayList;
import java.util.List;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Greg Hinkle
 * @author Jason Dobies
 */
public class Augeas {

    protected LibAugeas.Augeas_T augeas_t;

    private final Log log = LogFactory.getLog(this.getClass());

    public Augeas() {
        this("/", "/usr/local/share/augeas/lenses");
    }

    public Augeas(String rootPath, String loadPath) {
        // Paths to search for the libaugeas.so files. I've seen Ubuntu installation instructions for augeas
        // that encourage creating symbolic links in this directory, so this should be safe to rely on.
        NativeLibrary.addSearchPath("augeas", "/usr/local/lib");

        this.augeas_t = LibAugeas.INSTANCE.aug_init(rootPath, loadPath, LibAugeas.AugFlags.AUG_SAVE_BACKUP.getIndex());
    }

    public void outputTree(String path) {
        outputTree(path, 0);
    }

    public void outputTree(String path, int depth) {

        StringBuffer depthIndent = new StringBuffer();
        for (int i = 0; i < depth; i++)
            depthIndent.append("  ");

        log.info(depthIndent + path + " = " + get(path));

        for (String child : match(path + "/*")) {
            outputTree(child, depth + 1);
        }
    }

    public String get(String path) {
        PointerByReference ref = new PointerByReference();
        LibAugeas.INSTANCE.aug_get(augeas_t, path, ref);

        if (ref.getValue() != null) {
            return ref.getValue().getString(0);
        } else {
            return null;
        }
    }

    public List<String> match(String path) {
        PointerByReference pref = new PointerByReference();
        int matches = LibAugeas.INSTANCE.aug_match(augeas_t, path, pref);

        Pointer[] refs = pref.getValue().getPointerArray(0, matches);

        List<String> matchPaths = new ArrayList<String>();

        for (int i = 0; i < matches; i++) {
            matchPaths.add(refs[i].getString(0));
        }
        return matchPaths;
    }

    public void save() {
        int result = LibAugeas.INSTANCE.aug_save(augeas_t);
        if (result != 0) {
            throw new RuntimeException("Failed to save augeas changes");
        }
    }
}

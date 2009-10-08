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
package org.rhq.core.db.ant;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Encrypts a string. You can optionally have it base64 encode the MD5 string (which is the default). Set the "base64"
 * attribute to "false" if you do not want the string base64 encoded.
 */
public class MD5Task extends Task {
    private String value;
    private String property;
    private boolean base64 = true;

    public void setValue(String s) {
        value = s;
    }

    public void setProperty(String s) {
        property = s;
    }

    public void setBase64(boolean b) {
        base64 = b;
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        validateAttributes();

        MessageDigest message_digest;

        try {
            message_digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new BuildException(e); // should never occur; this would be bad - JRE has this builtin
        }

        byte[] md5_bytes = message_digest.digest(value.getBytes());

        String md5;

        if (base64) {
            md5 = Base64.encode(md5_bytes);
        } else {
            // put the md5 bytes in string form
            StringBuffer md5_string = new StringBuffer(md5_bytes.length * 2);

            for (int i = 0; i < md5_bytes.length; i++) {
                int hi = (md5_bytes[i] >> 4) & 0xf;
                int lo = md5_bytes[i] & 0xf;
                md5_string.append(Character.forDigit(hi, 16));
                md5_string.append(Character.forDigit(lo, 16));
            }

            md5 = md5_string.toString();
        }

        Project this_project = getProject();
        this_project.setNewProperty(property, md5);

        return;
    }

    private void validateAttributes() throws BuildException {
        if (value == null) {
            throw new BuildException("value==null");
        }

        if (property == null) {
            throw new BuildException("property==null");
        }
    }
}
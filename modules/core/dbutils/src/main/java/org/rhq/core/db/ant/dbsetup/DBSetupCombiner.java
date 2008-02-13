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
package org.rhq.core.db.ant.dbsetup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import mazz.i18n.Msg;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import org.rhq.core.db.ant.DbAntI18NFactory;
import org.rhq.core.db.ant.DbAntI18NResourceKeys;

/**
 * An ant task to combine multiple DBSetup XML files into a single file.
 */
public class DBSetupCombiner extends BaseFileSetTask {
    private static final Msg MSG = DbAntI18NFactory.getMsg();
    private static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private File m_destFile = null;
    private String m_order = null;
    private String m_name = null;
    private ArrayList<Environment.Variable> sysProps = new ArrayList<Environment.Variable>();

    public void setDestfile(File destFile) {
        m_destFile = destFile;
    }

    public void setOrder(String order) {
        m_order = order;
    }

    public void setName(String name) {
        m_name = name;
    }

    /**
     * Support subelements to set System properties e.g &lt;sysproperty key="foo" value="bar" /&gt; After the task has
     * completed, the system properties will be reverted to their old values (of if the system property didn't exist
     * before, it will be removed).
     *
     * @param sysprop
     */
    public void addSysproperty(Environment.Variable sysprop) {
        sysProps.add(sysprop);
    }

    /**
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        validateAttributes();

        // being able to set system properties can be useful to set JDBC LoggerDriver system properties
        // but remember the old values so we can revert back to them after the task finishes
        Properties old_sysprops = new Properties(); // old values for keys that existed
        List<String> nonexistent_sysprops = new ArrayList<String>(); // keys that didn't exist in system properties
        for (Environment.Variable env_var : sysProps) {
            String old_value = System.setProperty(env_var.getKey(), env_var.getValue());
            if (old_value == null) {
                nonexistent_sysprops.add(env_var.getKey());
            } else {
                old_sysprops.put(env_var.getKey(), old_value);
            }
        }

        try {
            List<File> files_to_combine;
            PrintWriter pw = null;

            // First get all the files, unsorted
            files_to_combine = getAllFiles();

            // Reorder according to user-specified order
            if (m_order != null) {
                List<String> order_list = new ArrayList<String>();
                StringTokenizer tok = new StringTokenizer(m_order, ", \n\r\t");

                while (tok.hasMoreTokens()) {
                    order_list.add(tok.nextToken());
                }

                // based on the specified order, we need to sort all the files
                Collections.sort(files_to_combine, new OrderComparator(order_list));
            }

            try {
                // Open the destFile
                pw = new PrintWriter(new FileWriter(m_destFile));

                // Write the prefix
                writePrefix(pw);

                // Concatenate the files
                catFiles(files_to_combine, pw);

                // Write the suffix
                writeSuffix(pw);
            } catch (IOException ioe) {
                throw new BuildException(ioe);
            } finally {
                if (pw != null) {
                    try {
                        pw.close();
                    } catch (Exception e) {
                    }
                }
            }
        } finally {
            // revert back to the old system properties
            for (String name : nonexistent_sysprops) {
                System.clearProperty(name);
            }

            for (Map.Entry old_entry : old_sysprops.entrySet()) {
                System.setProperty((String) old_entry.getKey(), (String) old_entry.getValue());
            }
        }

        return;
    }

    /**
     * This method concatenates a series of files to a single destination, stripping out XML prefixes and top-level
     * elements.
     *
     * @param  files A list of the files to be concatenated
     * @param  pw    Destination for writes.
     *
     * @throws IOException
     */
    private void catFiles(List<File> files, PrintWriter pw) throws IOException {
        BufferedReader in = null;

        try {
            for (int i = 0; i < files.size(); i++) {
                File current_file = files.get(i);
                String current_filename = current_file.getName();

                in = new BufferedReader(new InputStreamReader(new FileInputStream(current_file)));

                pw.println("<!-- BEGIN: " + current_filename + " -->");

                String line;

                while ((line = in.readLine()) != null) {
                    String trimmed_line = line.trim();

                    // Skip XML directive lines, and top-level element lines
                    if (trimmed_line.startsWith("<?") || trimmed_line.startsWith("</dbsetup>")) {
                        continue;
                    } else if (trimmed_line.startsWith("<dbsetup")) {
                        while (trimmed_line.indexOf(">") == -1) {
                            trimmed_line = in.readLine();
                            if (trimmed_line == null) {
                                break;
                            }
                        }

                        continue;
                    }

                    // print the original line
                    pw.println(line);
                }

                in.close();
                in = null;

                pw.println("<!-- END: " + current_filename + " -->");
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }

        return;
    }

    /**
     * Makes sure this task's attributes are valid.
     *
     * @see org.rhq.core.db.ant.dbsetup.BaseFileSetTask#validateAttributes()
     */
    protected void validateAttributes() throws BuildException {
        super.validateAttributes();

        if (m_destFile == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.TASK_MISSING_ATTRIB, getTaskName(), "destFile"));
        }

        if (m_name == null) {
            throw new BuildException(MSG.getMsg(DbAntI18NResourceKeys.TASK_MISSING_ATTRIB, getTaskName(), "name"));
        }
    }

    private void writePrefix(PrintWriter pw) throws IOException {
        pw.println(XML_PREFIX);
        pw.println("<dbsetup name=\"" + this.m_name + "\">");
    }

    private void writeSuffix(PrintWriter pw) throws IOException {
        pw.println("</dbsetup>");
    }

    private class OrderComparator implements Comparator {
        private List<String> orderList;

        /**
         * Creates a new {@link OrderComparator} object.
         *
         * @param order
         */
        public OrderComparator(List<String> order) {
            orderList = order;
        }

        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(Object o1, Object o2) {
            // Make sure they are both files, otherwise we'll just say they're always "equal"
            if ((o1 instanceof File) && (o2 instanceof File)) {
                // Get basenames for files
                String f1 = ((File) o1).getName();
                String f2 = ((File) o2).getName();

                // Find the index at which this basename occurs in the order
                int idx1 = findIndex(f1);
                int idx2 = findIndex(f2);

                // If one was found and the other was not, the one
                // that was found is automatically "less than" the one that was not.
                if ((idx1 == -1) && (idx2 != -1)) {
                    return Integer.MAX_VALUE;
                } else if ((idx2 == -1) && (idx1 != -1)) {
                    return Integer.MIN_VALUE;
                } else {
                    // If they were both found, or if they were both -1, just return the difference
                    return (idx1 - idx2);
                }
            }

            return 0;
        }

        private int findIndex(String fname) {
            int indexOfLongestMatch = -1;
            int lengthOfLongestMatch = 0;

            String possibleMatch;
            int possibleMatchLen;

            for (int i = 0; i < orderList.size(); i++) {
                possibleMatch = orderList.get(i).toString();
                possibleMatchLen = possibleMatch.length();

                if (fname.startsWith(possibleMatch) && (possibleMatchLen > lengthOfLongestMatch)) {
                    indexOfLongestMatch = i;
                    lengthOfLongestMatch = possibleMatchLen;
                }
            }

            return indexOfLongestMatch;
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof OrderComparator) {
                OrderComparator oc = (OrderComparator) o;
                return ((this.orderList == null) && (oc.orderList == null)) || this.orderList.equals(oc.orderList);
            }

            return false;
        }
    }
}
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
package org.rhq.core.system.pquery;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.rhq.core.system.NativeSystemInfo;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.pquery.Conditional.Qualifier;

/**
 * Performs a query over a set of {@link ProcessInfo#getCommandLine() command line strings}. The query strings are
 * written in the <i>Process Info Query Language</i> (PIQL, pronounced <i>pickle</i>).In effect, your PIQL will examine
 * a list of running processes whose command lines match a certain set of criteria. PIQL statements are formatted by a
 * series of <i>criteria</i>, with each criteria separated with a comma:
 *
 * <pre>CRITERIA[,CRITERIA]*</pre>
 *
 * <p>Criteria are formatted in the following manner:</p>
 *
 * <pre>CONDITIONAL=VALUE</pre>
 *
 * <p><i>VALUE</i> is either a regular expression or a pid filename to compare a value obtained using the <i>
 * CONDITIONAL</i>. See the class javadoc for <code>java.util.regex.Pattern</code> to learn the syntax of valid regular
 * expressions. A <i>CONDITIONAL</i> is defined as:</p>
 *
 * <pre>CATEGORY|ATTRIBUTE|OPERATOR[|QUALIFIER]</pre>
 *
 * <p>where:</p>
 *
 * <ul>
 *   <li><i>CATEGORY</i> is either <b>process</b> or <b>arg</b></li>
 *   <li><i>ATTRIBUTE</i> declares what is to be matched; value depends on the category - see below</li>
 *   <li><i>OPERATOR</i> is the conditional check made against the given <i>VALUE</i> - see below</li>
 *   <li><i>QUALIFIER</i> is an optional query flag; <code>parent</code> is the only value currently allowed</li>
 * </ul>
 *
 * <p>The <i>ATTRIBUTE</i> can be one of the following:</p>
 *
 * <ul>
 *   <li>If <i>CATEGORY</i> is <b>process</b>:
 *
 *     <ul>
 *       <li><b>name</b> - the full path of the executable (i.e. the full string of the first command line argument)
 *       </li>
 *       <li><b>basename</b> - just the executable filename (not including any path information)</li>
 *       <li><b>pidfile</b> - the contents of a file, assumed to be a single number representing a pid</li>
 *       <li><b>pid</b> - the pid itself (allows you to match a process in which you already know its pid)</li>
 *     </ul>
 *   </li>
 *   <li>If <i>CATEGORY</i> is <b>arg</b>:
 *
 *     <ul>
 *       <li><b>&lt;#></b> - a specific argument; this is an index in the process' command line arguments array (where
 *         argument 0 maps to the process name, -1 maps to the last argument)</li>
 *       <li><b>*</b> - a literal asterisk means any argument can match</li>
 *       <li><b>&lt;argname></b> - the name of the argument (e.g. "-b", "--port", "verbose")</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>The <i>OPERATOR</i> can be one of the following:</p>
 *
 * <ul>
 *   <li><b>match</b> - the <i>VALUE</i> regular expression must match</li>
 *   <li><b>nomatch</b> - the <i>VALUE</i> regular expression must not match</li>
 * </ul>
 *
 * <p>Some examples of PIQL are:</p>
 *
 * <table border="1">
 *   <tr>
 *     <th>PIQL</th>
 *     <th>What is matched</th>
 *   </tr>
 *   <tr>
 *     <td><code>process|pidfile|match=/etc/product/lock.pid</code></td>
 *     <td>the process whose pid matches the number found in the lock.pid file</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|pidfile|match|parent=/etc/product/lock.pid</code></td>
 *     <td>child processes of the parent process whose pid matches the number found in the lock.pid file</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|name|match=^/foo.*</code></td>
 *     <td>all processes whose executables are found under the root "foo" directory</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|basename|match=^java.*</code></td>
 *     <td>all processes whose executable file has "java" at the start of it</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|basename|match=(?i)^java.*</code></td>
 *     <td>all processes whose executable file has "java" at the start of it (case insensitive, so "JAVA" would also
 *       match)</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|name|match=.*(product|java).*</code></td>
 *     <td>all processes whose executable paths have either "product" or "java" in them</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|name|match=^C:.*,process|basename|nomatch=java.exe</code></td>
 *     <td>all processes whose executables are found on the Windows C: drive but is not a "java.exe" process</td>
 *   </tr>
 *   <tr>
 *     <td><code>arg|1|match=org\.jboss\.Main</code></td>
 *     <td>all processes whose command line argument #1 has a value of "org.jboss.Main". This will NOT match a process
 *       that does not have a command line argument at the given index.</td>
 *   </tr>
 *   <tr>
 *     <td><code>arg|*|match=.*daemon.*</code></td>
 *     <td>all processes whose command lines have any argument with the substring "daemon" in them</td>
 *   </tr>
 *   <tr>
 *     <td><code>arg|-b|nomatch=127\.0\.0\.1</code></td>
 *     <td>all processes whose command lines have any argument named "-b" whose value is not "127.0.0.1" (e.g. "-b
 *       192.168.0.5"). This will NOT match a process that does not have that argument at all.</td>
 *   </tr>
 *   <tr>
 *     <td><code>arg|-Dbind.address|match=127.0.0.1</code></td>
 *     <td>all processes whose command lines have any argument named "bind.address" whose value is "127.0.0.1" (e.g.
 *       "-Dbind.address=127.0.0.1"). This will NOT match a process that does not have that argument at all.</td>
 *   </tr>
 *   <tr>
 *     <td><code>arg|-cp|match=.*org\.abc\.Class.*</code></td>
 *     <td>all processes whose command lines have any argument named "-cp" whose value contains "org.abc.Class". This
 *       will NOT match a process that does not have that argument at all.</td>
 *   </tr>
 *   <tr>
 *     <td><code>arg|org.jboss.Main|match=.*</code></td>
 *     <td>all processes whose command lines have any argument named "org.jboss.Main"</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|basename|match=(?i)Apache.exe,arg|-k|match|parent=runservice</code></td>
 *     <td>all Apache processes that are running as child processes to the main Apache service.</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|basename|nomatch|parent=exec</code></td>
 *     <td>all processes that have a parent whose basename is not exec. This will match all processes that do not have a
 *       parent.</td>
 *   </tr>
 *   <tr>
 *
 *     <td>
 *       <code>process|basename|match=^(https?d.*|[Aa]pache)$,process|name|nomatch|parent=^(https?d.*|[Aa]pache)$</code>
 *     </td>
 *     <td>all Apache processes that do not have a parent process that is also an Apache process (i.e. this eliminates
 *       all of the httpd child processes and only returns the main Apache servers). This will match a process that does
 *       not have a parent but has a basename of Apache.</td>
 *   </tr>
 *   <tr>
 *     <td><code>process|pid|match=1016</code></td>
 *     <td>The process whose pid is 1016.</td>
 *   </tr>
 * </table>
 *
 * @author John Mazzitelli
 */
public class ProcessInfoQuery {
    /**
     * The map of all processes keyed on their pids.
     */
    private Map<Long, ProcessInfo> allProcesses;

    /**
     * Constructor for {@link ProcessInfoQuery} given an collection of process information that represents the processes
     * currently running. Think of the <code>processes</code> data as coming from part of the output you see in the
     * typical UNIX "ps" command.
     *
     * @param processes
     *
     * @see   NativeSystemInfo#getAllProcesses()
     */
    public ProcessInfoQuery(List<ProcessInfo> processes) {
        this.allProcesses = new HashMap<Long, ProcessInfo>(processes.size());
        for (ProcessInfo process : processes) {
            this.allProcesses.put(process.getPid(), process);
        }
    }

    /**
     * Returns the list of all the {@link ProcessInfo processes} that this object will {@link #query(String) query}
     * against.
     *
     * @return all processes this object knows about
     */
    public List<ProcessInfo> getProcesses() {
        return new ArrayList<ProcessInfo>(allProcesses.values());
    }

    /**
     * Performs a query on the set of known processes where <code>query</code> defines the criteria.
     *
     * @param  query the query string containing the criteria to match
     *
     * @return the matches processes' command lines
     *
     * @throws IllegalArgumentException if the query was invalid
     */
    public List<ProcessInfo> query(String query) {
        List<Criteria> criteriaList = getCriteriaList(query);

        // if we got an empty query - it means we match nothing so return an empty list immediately
        if (criteriaList.size() == 0) {
            return new ArrayList<ProcessInfo>();
        }

        // keyed on pid so we automatically avoid dups (in case more than one criteria matches)
        Map<Long, ProcessInfo> queryResults = new HashMap<Long, ProcessInfo>(this.allProcesses);
        Map<Long, ProcessInfo> criteriaResults;

        for (Criteria criteria : criteriaList) {
            if (criteria.getConditional().getCategory().equals(Conditional.Category.process)) {
                criteriaResults = doProcessCriteriaQuery(criteria);
            } else if (criteria.getConditional().getCategory().equals(Conditional.Category.arg)) {
                criteriaResults = doArgCriteriaQuery(criteria);
            } else {
                throw new IllegalArgumentException("Unknown category: " + criteria); // should never happen
            }

            // multiple criteria results are ANDed together
            // only retain those previously matched processes that were also matched in the latest criteria
            Set<Long> pids = new HashSet<Long>(queryResults.keySet()); // new set to avoid concurrent mod exceptions

            for (Long pid : pids) {
                if (!criteriaResults.containsKey(pid)) { // a previously matched process was not matched in the latest criteria, so removed it
                    queryResults.remove(pid);
                }
            }

            if (queryResults.size() == 0) {
                // we've eliminated every possible process - don't bother running any more criteria
                break;
            }
        }

        List<ProcessInfo> results = new ArrayList<ProcessInfo>(queryResults.size());
        results.addAll(queryResults.values());

        return results;
    }

    /**
     * Runs the given criteria with the arg conditional and returns the processes that match.
     *
     * @param  criteria the criteria with the arg conditional
     *
     * @return the matched processes keyed on the pids
     *
     * @throws IllegalArgumentException
     */
    private Map<Long, ProcessInfo> doArgCriteriaQuery(Criteria criteria) {
        Map<Long, ProcessInfo> matches = new HashMap<Long, ProcessInfo>();

        Attribute attribute = criteria.getConditional().getAttribute();
        Operation op = new Operation(criteria.getConditional().getOperator());
        Qualifier qualifier = criteria.getConditional().getQualifier();

        String operand1 = null;
        String operand2 = criteria.getValue();

        for (ProcessInfo process : getProcesses()) {
            ProcessInfo processToMatch; // will be the same as process unless the parent qualifier was provided

            if (qualifier.equals(Qualifier.parent)) {
                processToMatch = getParentProcess(process);
            } else {
                processToMatch = process;
            }

            String[] cmdline = (processToMatch != null) ? processToMatch.getCommandLine() : null;

            if ((cmdline == null) || (cmdline.length == 0)) {
                continue; // no sense continuing with this process - there are no command line arguments
            }

            if (attribute.getAttributeValue().equals("*")) {
                // * means see if any arg matches
                for (String arg : cmdline) {
                    operand1 = arg;
                    if (op.doOperation(operand1, operand2)) {
                        matches.put(process.getPid(), process);
                        break; // we got a match, don't bother looking at more args
                    }
                }
            } else if (attribute.getAttributeValueAsInteger() != null) {
                // if we get here, it means the argument specified was a specific argument index number
                int attributeIndex = attribute.getAttributeValueAsInteger().intValue();

                // an arg of -1 means the query wants to obtain the last argument in the command line
                if (attributeIndex < 0) {
                    attributeIndex = cmdline.length - 1;
                }

                if ((cmdline.length - 1) < attributeIndex) {
                    continue; // process doesn't have enough args - there is no command line argument with that index
                }

                operand1 = cmdline[attributeIndex];

                if (op.doOperation(operand1, operand2)) {
                    matches.put(process.getPid(), process);
                }
            } else {
                // if we get here, it means the attribute specified was the name of an argument
                String attributeName = attribute.getAttributeValue();

                for (int i = 0; i < cmdline.length; i++) {
                    String arg = cmdline[i];

                    // if the arg name doesn't even start with our attribute, then we continue on to the next
                    if (arg.startsWith(attributeName)) {
                        if (arg.equals(attributeName)) {
                            // the full argument name is the attribute name, the command line was something like:
                            // "exec.exe -arg value" or "exec.exe -arg" so the value is the next argument
                            operand1 = ((i + 1) < cmdline.length) ? cmdline[i + 1] : "";
                        } else {
                            // the command line was something like: "exec.exe -arg=value" so the value is after the equals side within the arg
                            int equals = arg.indexOf('=');
                            if (equals == -1) {
                                continue; // the argument looked like what we were trying to find, but it really wasn't
                            }

                            operand1 = (arg.length() > (equals + 1)) ? arg.substring(equals + 1) : "";
                        }

                        if (op.doOperation(operand1, operand2)) {
                            matches.put(process.getPid(), process);
                            break; // no need to continue, we've got the match we are looking for
                        }
                    }
                }
            }
        }

        return matches;
    }

    /**
     * Runs the given criteria with the process conditional and returns the processes that match.
     *
     * @param  criteria the criteria with the process conditional
     *
     * @return the matched processes keyed on the pids
     *
     * @throws IllegalArgumentException
     */
    private Map<Long, ProcessInfo> doProcessCriteriaQuery(Criteria criteria) {
        Map<Long, ProcessInfo> matches = new HashMap<Long, ProcessInfo>();

        Attribute attribute = criteria.getConditional().getAttribute();
        Operation op = new Operation(criteria.getConditional().getOperator());
        Qualifier qualifier = criteria.getConditional().getQualifier();

        String operand1;
        String operand2;

        String pidfileContentsCache = null; // so we avoid reading the file over and over again

        for (ProcessInfo process : getProcesses()) {
            ProcessInfo processToMatch; // will be the same as process unless the parent qualifier was provided

            if (qualifier.equals(Qualifier.parent)) {
                processToMatch = getParentProcess(process);
            } else {
                processToMatch = process;
            }

            if (attribute.getAttributeValue().equals(Attribute.ProcessCategoryAttributes.name.toString())) {
                operand1 = (processToMatch != null) ? processToMatch.getName() : "";
                operand2 = criteria.getValue();
            } else if (attribute.getAttributeValue().equals(Attribute.ProcessCategoryAttributes.basename.toString())) {
                operand1 = (processToMatch != null) ? processToMatch.getBaseName() : "";
                operand2 = criteria.getValue();
            } else if (attribute.getAttributeValue().equals(Attribute.ProcessCategoryAttributes.pid.toString())) {
                operand1 = (processToMatch != null) ? Long.toString(processToMatch.getPid()) : "";
                operand2 = criteria.getValue();
            } else if (attribute.getAttributeValue().equals(Attribute.ProcessCategoryAttributes.pidfile.toString())) {
                if (pidfileContentsCache == null) {
                    pidfileContentsCache = getPidfileContents(criteria.getValue());
                }

                operand1 = (processToMatch != null) ? String.valueOf(processToMatch.getPid()) : null;
                operand2 = pidfileContentsCache;
            } else {
                throw new IllegalArgumentException(
                    "Criteria with 'process' category must have an attribute of either 'name' or 'basename': "
                        + criteria);
            }

            if (op.doOperation(operand1, operand2)) {
                matches.put(process.getPid(), process);
            }
        }

        return matches;
    }

    /**
     * Gets the parent process for the given process. The parent will be searched for within the {@link #getProcesses()}
     * list.
     *
     * @param  child
     *
     * @return the child's parent process or <code>null</code> if the child has no parent
     */
    private ProcessInfo getParentProcess(ProcessInfo child) {
        ProcessInfo parent = null;

        if (child != null) {
            parent = this.allProcesses.get(child.getParentPid());
        }

        return parent;
    }

    private List<Criteria> getCriteriaList(String query) {
        List<Criteria> criteria = new ArrayList<Criteria>();

        if (query != null) {
            String[] tokens = query.split(",");

            for (String criteriaString : tokens) {
                Criteria c = new Criteria(criteriaString);
                criteria.add(c);
            }
        }

        return criteria;
    }

    private String getPidfileContents(String pidfileName) {
        String contents;

        try {
            byte[] bytes = new byte[64]; // a pid file should never come close to being 64 bytes big
            FileInputStream fis = new FileInputStream(pidfileName);
            try {
                int count = fis.read(bytes);
                contents = new String(bytes, 0, count);
            } finally {
                fis.close();
            }
        } catch (Exception e) {
            contents = "";
        }

        return contents;
    }
}
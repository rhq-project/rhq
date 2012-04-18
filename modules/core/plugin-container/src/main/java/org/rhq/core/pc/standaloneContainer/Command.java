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
package org.rhq.core.pc.standaloneContainer;

import java.util.EnumSet;

/**
 * List of possible commands of the standalone container
 * @author Heiko W. Rupp
 */
public enum Command {
    ASCAN("as", "", 0, "Triggers an availability scan"), //
    AVAIL("a", " ( id )", 0,
        "Shows an availability report. If id is given, only shows availability for resource with id id. To name the current id, you can use a single dot"), //
    CHILDREN("chi", "[id]", 0,
        "Shows the direct children of the resource with the passed id, or if no id passed of the current resource"), //
    DISCOVER("disc", " s | i | all", 1, "Triggers a discovery scan for (s)erver, serv(i)ce or all resources"), //
    //      EVENT("e", "", 0,  "Pull events"), // TODO needs to be defined
    FIND("find", "r | t  | rt <name>", 2,
        "Searches a (r)esource, resource (t)ype or resources of (rt)ype. Use * as wildcard.\n"
            + " Will set $r for the last resource shown."),//
    HELP("h", "", 0, "Shows this help"), //
    INVOKE(
        "i",
        "operation [params]",
        1,
        "Triggers running an operation. If operation is '-list' it shows available operations.\n Parameters are given as key=value; key-value-pairs are separated by ||"), //
    MEASURE(
        "m",
        "datatype property+",
        1,
        "Triggers getting metric values. All need to be of the same data type. If datatype is '-list' it shows the defined metrics"), //
    NATIVE("n", "e | d | s", 1, "Enables/disables native system or shows native status"), //
    PRINT("p", "[id]", 0,
        "Prints information about the resource with id 'id'. If no id is given, the current resource is printed."), //
    QUIT("quit", "", 0, "Terminates the application"), //
    RESOURCES("res", "", 0, "Shows the discovered resources"), //
    SET(
        "set",
        "'resource' N",
        2,
        "Sets the resource id to work with. N can be a number or '$r' as result of last find resource call. 'id' is an alias for 'res'"), //
    STDIN("stdin", "", 0, "Stop reading the batch file and wait for commands on stdin"), //
    WAIT("w", "milliseconds", 1, "Waits the given amount of time"),
    P_CONFIG("pc", "( property name(s) )", 0,
        "Shows the plugin configuration of the current resource. If property names (separated by comma) are given, only show those properties"),
    R_CONFIG("rc", "", 0,
        "Shows the resource configuration of the current resource."),
    SR_CONFIG(
        "rcs",
        "",
        1,
        "[-m] [parameters] set resource config. '-m' merges with current config; default is overwrite. Properties are separated by ||."),
    SP_CONFIG(
        "pcs",
        "",
        1,
        "[-m] [parameters] set plugin config.'-m' merges with current config; default is overwrite. Properties are separated by ||.");

    private String abbrev;
    private String args;
    private String help;
    private int minArgs; // minimum number of args needed

    public String getArgs() {
        return args;
    }

    public String getHelp() {
        return help;
    }

    public int getMinArgs() {
        return minArgs;
    }

    /**
     * Construct a new Command
     * @param abbrev Abbreviation for this command
     * @param args Description of expected arguments
     * @param minArgs Minumum number of arguments that need to be present
     * @param help A short description of the command
     */
    private Command(String abbrev, String args, int minArgs, String help) {
        this.abbrev = abbrev;
        this.args = args;
        this.minArgs = minArgs;
        this.help = help;
    }

    public String getAbbrev() {
        return abbrev;
    }

    public static Command get(String s) {

        String upper = s.toUpperCase();

        for (Command c : EnumSet.allOf(Command.class)) {
            if (c.name().equals(upper) || c.getAbbrev().equals(s.toLowerCase()))
                return c;
        }
        return null;
    }
}

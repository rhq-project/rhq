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
package org.rhq.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.testng.annotations.Test;

@Test
public class IniEditorTest {
    public void testLoad() throws Exception {
        String content = "[one]\n" + //
            "\tfirst=value1\n" + //
            "    second=value2\n" + //
            "   [Two]   \n" + //
            "# comment here\n" + //
            "    # spaces then comment    \n" + //
            "\t# tab then comment \n" + //
            "\n" + //
            "foo = bar\n";

        IniEditor ini = new IniEditor(new char[] { '#' }, true);
        ini.load(new ByteArrayInputStream(content.getBytes()));
        assert ini.get("one", "first").equals("value1");
        assert ini.get("one", "second").equals("value2");
        assert ini.get("Two", "foo").equals("bar");
        assert ini.hasSection("one") : ini.sectionNames();
        assert ini.hasSection("Two") : ini.sectionNames();
        assert !ini.hasSection("two") : "should have been case sensitive";
        assert ini.sectionNames().size() == 2 : ini.sectionNames();
        assert ini.optionNames("one").size() == 2 : ini.optionNames("one");
        assert ini.optionNames("Two").size() == 1 : ini.optionNames("Two");

        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        ini.save(boas);
        System.out.println(boas.toString());
    }

    public void testLoad2() throws Exception {
        // tests an actual puppet config file
        IniEditor ini = new IniEditor(new char[] { '#' }, true);
        ini.load(this.getClass().getClassLoader().getResourceAsStream("ini-editor-test.ini"));
        assert ini.sectionNames().size() == 4 : ini.sectionNames();
        assert ini.sectionNames().get(0).equals("main");
        assert ini.sectionNames().get(1).equals("puppetd");
        assert ini.sectionNames().get(2).equals("puppetca");
        assert ini.sectionNames().get(3).equals("puppetmasterd");

        assert ini.optionNames("main").size() == 4 : ini.optionNames("main");
        assert ini.optionNames("puppetd").size() == 2 : ini.optionNames("puppetd");
        assert ini.optionNames("puppetca").size() == 1 : ini.optionNames("puppetca");
        assert ini.optionNames("puppetmasterd").size() == 2 : ini.optionNames("puppetmasterd");

        assert ini.get("main", "vardir").equals("/var/lib/puppet");
        assert ini.get("main", "logdir").equals("/var/log/puppet");
        assert ini.get("main", "rundir").equals("/var/run/puppet");
        assert ini.get("main", "ssldir").equals("$vardir/ssl");
        assert ini.get("puppetd", "classfile").equals("$vardir/classes.txt");
        assert ini.get("puppetd", "localconfig").equals("$vardir/localconfig");
        assert ini.get("puppetca", "autosign").equals("false");
        assert ini.get("puppetmasterd", "ca").equals("true");
        assert ini.get("puppetmasterd", "certname").equals("mycertname");
    }
}

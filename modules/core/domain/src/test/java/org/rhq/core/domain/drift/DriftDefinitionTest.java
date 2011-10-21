/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.domain.drift;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
import org.rhq.core.domain.drift.DriftDefinitionComparator.CompareMode;

public class DriftDefinitionTest {
    @Test
    public void getCompareIgnoreIncludesExcludes() {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(CompareMode.ONLY_BASE_INFO);

        DriftDefinition dc1 = new DriftDefinition(new Configuration());
        DriftDefinition dc2 = new DriftDefinition(new Configuration());

        // make sure our comparator can deal with all the nulls that are in empty configs
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2;
        dc1.setEnabled(false);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setInterval(1000L);
        dc1.setDriftHandlingMode(DriftHandlingMode.normal);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setName("the-name");
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.setEnabled(false);
        dc2.setInterval(1000L);
        dc2.setDriftHandlingMode(DriftHandlingMode.normal);
        dc2.setName("the-name");

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2;

        dc1.setEnabled(!dc2.isEnabled());
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different enabled than " + dc2;

        dc1.setEnabled(dc2.isEnabled()); // put them back to the same value
        dc1.setInterval(dc2.getInterval() + 2222L);
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different interval than " + dc2;

        dc1.setInterval(dc2.getInterval()); // put them back to the same value
        dc1.setDriftHandlingMode(DriftHandlingMode.plannedChanges);
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different drift handling mode than " + dc2;

        dc1.setDriftHandlingMode(DriftHandlingMode.normal); // put them back to the same value

        // change the description of dc1
        dc1.setDescription("description 1");
        assert comparator.compare(dc1, dc2) > 0 : dc1 + " description is different from dc2's";

        // set the descriptions to be the same
        dc2.setDescription(dc1.getDescription());
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " description should equal description of " + dc2;

        // make dc1 description null
        dc1.setDescription(null);
        assert comparator.compare(dc1, dc2) < 0 : dc1 + " description is null and " + dc2 + " description is not null";

        // make both dc1 and dc2 descriptions non-null and different
        dc1.setDescription("description 1");
        dc2.setDescription("description 2");
        assert comparator.compare(dc1, dc2) < 0 : dc1 + " description is different from description of " + dc2;

        // make descriptions same again
        dc1.setDescription(null);
        dc2.setDescription(null);

        // make dc1 detached
        dc1.setAttached(false);
        assert comparator.compare(dc1, dc2) < 0 : dc1 + " is not attached";

        // make dc1 attached and dc2 detached
        dc1.setAttached(true);
        dc2.setAttached(false);
        assert comparator.compare(dc1, dc2) > 0 : dc2 + " is not attached";

        // make both dc1 and dc2 detached
        dc1.setAttached(false);
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " and " + dc2 + " are both detached";

        // Make dc1 pinned
        dc1.setPinned(true);
        assert comparator.compare(dc1, dc2) > 0 : dc1 + " is pinned";

        // make both dc1 and dc2 pinned
        dc2.setPinned(true);
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should be equal to " + dc2;

        dc1.setName("zzzzz" + dc2.getName());
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different name than " + dc2;

        dc1.setName(dc2.getName()); // put them back to the same value
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check - we should be back to equals

        // add some includes and excludes that are different and test that they are ignored by our comparator
        dc1.setBasedir(new DriftDefinition.BaseDirectory(BaseDirValueContext.fileSystem, "/foo"));
        dc2.setBasedir(new DriftDefinition.BaseDirectory(BaseDirValueContext.pluginConfiguration, "blah"));
        dc1.addInclude(new Filter("ipath1", "ipattern1"));
        dc2.addInclude(new Filter("ipath2", "ipattern2"));
        dc1.addExclude(new Filter("epath1", "epattern1"));
        dc2.addExclude(new Filter("epath2", "epattern2"));

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal (ignoring basedir/includes/excludes) " + dc2;

        // now show that our non-ignoring comparator would detect a different
        comparator = new DriftDefinitionComparator(CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal (not ignoring basedir/includes/excludes) "
            + dc2;
    }

    @Test
    public void getCompareBaseInfoAndIncludesExcludes() {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(
            CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);

        DriftDefinition dc1 = new DriftDefinition(new Configuration());
        DriftDefinition dc2 = new DriftDefinition(new Configuration());

        dc1.setEnabled(true);
        dc1.setInterval(1000L);
        dc1.setDriftHandlingMode(DriftHandlingMode.normal);
        dc1.setName("the-name");
        dc1.setPinned(true);

        dc2.setEnabled(true);
        dc2.setInterval(1000L);
        dc2.setDriftHandlingMode(DriftHandlingMode.normal);
        dc2.setName("the-name");
        dc2.setPinned(true);

        getCompareBaseInfoAndIncludesExcludes(comparator, dc1, dc2);
    }

    @Test
    public void getCompareOnlyIncludesExcludes() {
        DriftDefinitionComparator comparator = new DriftDefinitionComparator(CompareMode.ONLY_DIRECTORY_SPECIFICATIONS);

        DriftDefinition dc1 = new DriftDefinition(new Configuration());
        DriftDefinition dc2 = new DriftDefinition(new Configuration());

        dc1.setEnabled(false);
        dc1.setInterval(1111L);
        dc2.setDriftHandlingMode(DriftHandlingMode.normal);
        dc1.setName("some-name");

        dc2.setEnabled(true);
        dc2.setInterval(2222L);
        dc2.setDriftHandlingMode(DriftHandlingMode.plannedChanges);
        dc2.setName("another-name");

        getCompareBaseInfoAndIncludesExcludes(comparator, dc1, dc2);
    }

    /**
     * Used by two main tests - we are to pass in a comparator that compares both base info
     * and filters (with dc1 and dc2 having the same base info to make sure that data is compared too)
     * and then we are to pass in a comparator that compares only the filters (with dc1 and
     * dc2 having different base info to make sure that data is ignored).
     * 
     * Note that when first called, comparator is assumed to see that dc1 and dc2 are the same.
     *
     * @param comparator used to test changes in filters
     * @param dc1 the initial drift definition1 to test
     * @param dc2 the initial drift definition2 to test
     */
    private void getCompareBaseInfoAndIncludesExcludes(DriftDefinitionComparator comparator, DriftDefinition dc1,
        DriftDefinition dc2) {

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check

        dc1.setBasedir(new DriftDefinition.BaseDirectory(BaseDirValueContext.pluginConfiguration, "hello.world"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.setBasedir(new DriftDefinition.BaseDirectory(BaseDirValueContext.pluginConfiguration, "hello.world"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        dc1.addInclude(new Filter("ipath1", "ipattern1"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addInclude(new Filter("ipath1", "ipattern1"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // add a second include to see that we test multiple filters
        dc1.addInclude(new Filter("ipath2", "ipattern2"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addInclude(new Filter("ipath2", "ipattern2"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // side test just to see null patterns work
        dc1.addInclude(new Filter("ipath3", null));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addInclude(new Filter("ipath3", null));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // now test excludes

        dc1.addExclude(new Filter("epath1", "epattern1"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addExclude(new Filter("epath1", "epattern1"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // add a second exclude to see that we test multiple filters
        dc1.addExclude(new Filter("epath2", "epattern2"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.addExclude(new Filter("epath2", "epattern2"));
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should now be equal " + dc2;
        assert comparator.compare(dc2, dc1) == 0 : dc2 + " should now be equal " + dc1;

        // now test that we have the same number of filters but they differ

        dc1.addInclude(new Filter("ipathA", "ipatternA"));
        dc2.addInclude(new Filter("ipathZ", "ipatternZ"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        // we don't provide an API to clear filters, so just create new drift definitions and test different excludes
        dc1 = new DriftDefinition(new Configuration());
        dc2 = new DriftDefinition(new Configuration());
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check
        dc1.addExclude(new Filter("epathA", "epatternA"));
        dc2.addExclude(new Filter("epathZ", "epatternZ"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
    }

    @Test
    public void getName() {
        String name = "test";
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));

        DriftDefinition driftDef = new DriftDefinition(config);

        assertEquals(driftDef.getName(), name, "Failed to get drift definition name");
    }

    @Test
    public void getBasedirForFileSystemContext() {
        String basedir = "/opt/drift/test";
        Configuration config = new Configuration();

        PropertyMap map = new PropertyMap(PROP_BASEDIR);
        map.put(new PropertySimple(PROP_BASEDIR_VALUECONTEXT, fileSystem));
        map.put(new PropertySimple(PROP_BASEDIR_VALUENAME, basedir));

        config.put(map);

        DriftDefinition driftDef = new DriftDefinition(config);

        assertEquals(driftDef.getBasedir().getValueName(), basedir, "Failed to get drift definition base directory");
    }

    @Test
    public void getInterval() {
        long interval = 3600L;
        Configuration config = new Configuration();
        config.put(new PropertySimple("interval", interval));

        DriftDefinition driftDef = new DriftDefinition(config);

        assertEquals(driftDef.getInterval(), interval, "Failed to get drift definition interval");
    }

    @Test
    public void getDriftHandlingMode() {
        DriftHandlingMode mode = DriftHandlingMode.normal;
        Configuration config = new Configuration();
        config.put(new PropertySimple(DriftConfigurationDefinition.PROP_DRIFT_HANDLING_MODE, mode.name()));
        DriftDefinition driftDef = new DriftDefinition(config);
        assertEquals(driftDef.getDriftHandlingMode(), mode, "Failed to get drift definition drift handling mode");

        mode = DriftHandlingMode.plannedChanges;
        driftDef.setDriftHandlingMode(mode);
        assertEquals(driftDef.getDriftHandlingMode(), mode, "Failed to get drift definition drift handling mode");
    }

    @Test
    public void isPinned() {
        Configuration config = new Configuration();
        config.put(new PropertySimple(DriftConfigurationDefinition.PROP_PINNED, true));
        DriftDefinition driftDef = new DriftDefinition(config);
        assertTrue(driftDef.isPinned(), "Expected drift definition to be pinned");

        driftDef.setPinned(false);
        assertFalse(driftDef.isPinned(), "Expected drift definition not to be pinned");
    }

    @Test
    public void getIncludes() {
        String path1 = "lib";
        String pattern1 = "*.jar";

        String path2 = "conf";
        String pattern2 = "*.xml";

        Configuration config = new Configuration();

        PropertyList includes = new PropertyList("includes");
        includes.add(newInclude(path1, pattern1));
        includes.add(newInclude(path2, pattern2));

        config.put(includes);

        DriftDefinition driftDef = new DriftDefinition(config);
        List<Filter> actual = driftDef.getIncludes();

        List<Filter> expected = asList(new Filter(path1, pattern1), new Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two includes filters");
        assertEquals(actual, expected, "Failed to get drift definition includes filters");
    }

    @Test
    public void getExcludes() {
        String path1 = "lib";
        String pattern1 = "*.jar";

        String path2 = "conf";
        String pattern2 = "*.xml";

        Configuration config = new Configuration();

        PropertyList excludes = new PropertyList("excludes");
        excludes.add(newExclude(path1, pattern1));
        excludes.add(newExclude(path2, pattern2));

        config.put(excludes);

        DriftDefinition driftDef = new DriftDefinition(config);
        List<Filter> actual = driftDef.getExcludes();

        List<Filter> expected = asList(new Filter(path1, pattern1), new Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two excludes filters");
        assertEquals(actual, expected, "Failed to get drift definition excludes filters");
    }

    private PropertyMap newInclude(String path, String pattern) {
        return new PropertyMap("include", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }

    private PropertyMap newExclude(String path, String pattern) {
        return new PropertyMap("exclude", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }
}

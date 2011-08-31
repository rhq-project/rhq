package org.rhq.core.domain.drift;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfigurationComparator.CompareMode;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME;
import static org.testng.Assert.assertEquals;

public class DriftConfigurationTest {
    @Test
    public void getCompareIgnoreIncludesExcludes() {
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(CompareMode.ONLY_BASE_INFO);

        DriftConfiguration dc1 = new DriftConfiguration(new Configuration());
        DriftConfiguration dc2 = new DriftConfiguration(new Configuration());

        // make sure our comparator can deal with all the nulls that are in empty configs
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2;
        dc1.setEnabled(false);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setInterval(1000L);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;
        dc1.setName("the-name");
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.setEnabled(false);
        dc2.setInterval(1000L);
        dc2.setName("the-name");

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2;

        dc1.setEnabled(!dc2.isEnabled());
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different enabled than " + dc2;

        dc1.setEnabled(dc2.isEnabled()); // put them back to the same value
        dc1.setInterval(dc2.getInterval() + 2222L);
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different interval than " + dc2;

        dc1.setInterval(dc2.getInterval()); // put them back to the same value
        dc1.setName("zzzzz" + dc2.getName());
        assert comparator.compare(dc1, dc2) > 0 : dc1 + "  should have different name than " + dc2;

        dc1.setName(dc2.getName()); // put them back to the same value
        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check - we should be back to equals

        // add some includes and excludes that are different and test that they are ignored by our comparator
        dc1.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.fileSystem, "/foo"));
        dc2.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.pluginConfiguration, "blah"));
        dc1.addInclude(new Filter("ipath1", "ipattern1"));
        dc2.addInclude(new Filter("ipath2", "ipattern2"));
        dc1.addExclude(new Filter("epath1", "epattern1"));
        dc2.addExclude(new Filter("epath2", "epattern2"));

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal (ignoring basedir/includes/excludes) " + dc2;

        // now show that our non-ignoring comparator would detect a different
        comparator = new DriftConfigurationComparator(CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal (not ignoring basedir/includes/excludes) "
            + dc2;
    }

    @Test
    public void getCompareBaseInfoAndIncludesExcludes() {
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(
            CompareMode.BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS);

        DriftConfiguration dc1 = new DriftConfiguration(new Configuration());
        DriftConfiguration dc2 = new DriftConfiguration(new Configuration());

        dc1.setEnabled(true);
        dc1.setInterval(1000L);
        dc1.setName("the-name");

        dc2.setEnabled(true);
        dc2.setInterval(1000L);
        dc2.setName("the-name");

        getCompareBaseInfoAndIncludesExcludes(comparator, dc1, dc2);
    }

    @Test
    public void getCompareOnlyIncludesExcludes() {
        DriftConfigurationComparator comparator = new DriftConfigurationComparator(
            CompareMode.ONLY_DIRECTORY_SPECIFICATIONS);

        DriftConfiguration dc1 = new DriftConfiguration(new Configuration());
        DriftConfiguration dc2 = new DriftConfiguration(new Configuration());

        dc1.setEnabled(false);
        dc1.setInterval(1111L);
        dc1.setName("some-name");

        dc2.setEnabled(true);
        dc2.setInterval(2222L);
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
     * @param dc1 the initial drift config1 to test
     * @param dc2 the initial drift config2 to test
     */
    private void getCompareBaseInfoAndIncludesExcludes(DriftConfigurationComparator comparator, DriftConfiguration dc1,
        DriftConfiguration dc2) {

        assert comparator.compare(dc1, dc2) == 0 : dc1 + " should equal " + dc2; // sanity check

        dc1.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.pluginConfiguration, "hello.world"));
        assert comparator.compare(dc1, dc2) != 0 : dc1 + " should not equal " + dc2;
        assert comparator.compare(dc2, dc1) != 0 : dc2 + " should not equal " + dc1;

        dc2.setBasedir(new DriftConfiguration.BaseDirectory(BaseDirValueContext.pluginConfiguration, "hello.world"));
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

        // we don't provide an API to clear filters, so just create new drift configs and test different excludes
        dc1 = new DriftConfiguration(new Configuration());
        dc2 = new DriftConfiguration(new Configuration());
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

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getName(), name, "Failed to get drift configuration name");
    }

    @Test
    public void getBasedirForFileSystemContext() {
        String basedir = "/opt/drift/test";
        Configuration config = new Configuration();

        PropertyMap map = new PropertyMap(PROP_BASEDIR);
        map.put(new PropertySimple(PROP_BASEDIR_VALUECONTEXT, fileSystem));
        map.put(new PropertySimple(PROP_BASEDIR_VALUENAME, basedir));

        config.put(map);

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getBasedir().getValueName(), basedir,
            "Failed to get drift configuration base directory");
    }

    @Test
    public void getInterval() {
        long interval = 3600L;
        Configuration config = new Configuration();
        config.put(new PropertySimple("interval", interval));

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getInterval(), interval, "Failed to get drift configuration interval");
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

        DriftConfiguration driftConfig = new DriftConfiguration(config);
        List<Filter> actual = driftConfig.getIncludes();

        List<Filter> expected = asList(new Filter(path1, pattern1), new Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two includes filters");
        assertEquals(actual, expected, "Failed to get drift configuration includes filters");
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

        DriftConfiguration driftConfig = new DriftConfiguration(config);
        List<Filter> actual = driftConfig.getExcludes();

        List<Filter> expected = asList(new Filter(path1, pattern1), new Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two excludes filters");
        assertEquals(actual, expected, "Failed to get drift configuration excludes filters");
    }

    private PropertyMap newInclude(String path, String pattern) {
        return new PropertyMap("include", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }

    private PropertyMap newExclude(String path, String pattern) {
        return new PropertyMap("exclude", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }
}

package org.rhq.core.domain.drift;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

import static java.util.Arrays.asList;
import static org.testng.Assert.*;

public class DriftConfigurationTest {
    @Test
    public void getName() {
        String name = "test";
        Configuration config = new Configuration();
        config.put(new PropertySimple("name", name));

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getName(), name, "Failed to get drift configuration name");
    }

    @Test
    public void getBasedir() {
        String basedir = "/opt/drift/test" ;
        Configuration config = new Configuration();
        config.put(new PropertySimple("basedir", basedir));

        DriftConfiguration driftConfig = new DriftConfiguration(config);

        assertEquals(driftConfig.getBasedir(), basedir, "Failed to get drift configuration base directory");
    }

    @Test
    public void getInterval() {
        Long interval = 3600L;
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
        List<DriftConfiguration.Filter> actual = driftConfig.getIncludes();

        List<DriftConfiguration.Filter> expected = asList(new DriftConfiguration.Filter(path1, pattern1),
            new DriftConfiguration.Filter(path2, pattern2));

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
        List<DriftConfiguration.Filter> actual = driftConfig.getExcludes();

        List<DriftConfiguration.Filter> expected = asList(new DriftConfiguration.Filter(path1, pattern1),
            new DriftConfiguration.Filter(path2, pattern2));

        assertEquals(actual.size(), 2, "Expected to find two excludes filters");
        assertEquals(actual, expected, "Failed to get drift configuration excludes filters");
    }

    PropertyMap newInclude(String path, String pattern) {
        return new PropertyMap("include", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }

    PropertyMap newExclude(String path, String pattern) {
        return new PropertyMap("exclude", new PropertySimple("path", path), new PropertySimple("pattern", pattern));
    }
}

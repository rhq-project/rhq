package org.rhq.core.domain.configuration;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * @author Lukas Krejci
 */
@Test
public class ConfigurationBuilderTest {

    public void testConfigurationProperties() {
        Configuration config = Configuration.builder().withNotes("notes").withVersion(2).build();
        assert "notes".equals(config.getNotes()) : "Unexpected notes";
        assert 2 == config.getVersion() : "Unexpected version";
    }

    public void testSimples() {
        Configuration config = Configuration.builder().addSimple("1", 1).addSimple("2", 2).build();

        assert config.getSimpleValue("1").equals("1") : "1 != 1";
        assert config.getSimpleValue("2").equals("2") : "2 != 2";

        assert config.getSimple("1").getConfiguration() == config : "Configuration not set on property";
    }

    public void testListOfSimples() {
        Configuration config = Configuration.builder().openList("l1", "m").addSimple(1).addSimple(2).closeList()
            .openList("l2", "m").addSimples(1, 2).closeList().build();

        PropertyList l1 = config.getList("l1");
        PropertyList l2 = config.getList("l2");

        assert l1 != null : "Could not find l1";
        assert l2 != null : "Could not find l2";

        assert l1.getConfiguration() == config : "Configuration on l1 not set";
        assert l2.getConfiguration() == config : "Configuration on l2 not set";

        assert l1.getList().size() == 2 : "Unexpected number of props in the list 1";
        assert l2.getList().size() == 2 : "Unexpected number of props in the list 2";

        testSimple((PropertySimple) l1.getList().get(0), null, l1, "1");
        testSimple((PropertySimple) l1.getList().get(1), null, l1, "2");
        testSimple((PropertySimple) l2.getList().get(0), null, l2, "1");
        testSimple((PropertySimple) l2.getList().get(1), null, l2, "2");
    }

    public void testMapOfSimples() {
        Configuration config = Configuration.builder().openMap("m").addSimple("1", 1).addSimple("2", 2).closeMap()
            .build();

        PropertyMap m = config.getMap("m");

        assert m != null : "Cound not find map";

        assert m.getConfiguration() == config : "Configuration on the map not set";

        testSimple(m.getSimple("1"), m, null, "1");
        testSimple(m.getSimple("2"), m, null, "2");
    }

    public void testListOfLists() {
        Configuration config = new Configuration.Builder().openList("l", "m").openList("ml1").addSimples(1, 2)
            .closeList().openList("ml2").addSimple(1).addSimple(2).closeList().closeList().build();

        PropertyList l = config.getList("l");

        assert l != null : "Could not find top-level list";

        assert l.getList().size() == 2 : "Unexpected number of props in the top level list";

        PropertyList m1 = (PropertyList) l.getList().get(0);
        PropertyList m2 = (PropertyList) l.getList().get(1);

        assert m1.getParentList() == l : "Parent list on m1 not set";
        assert m1.getParentMap() == null : "Unexpected parent map on m1";
        assert m2.getParentList() == l : "Parent list on m2 not set";
        assert m1.getParentMap() == null : "Unexpected parent map on m2";

        assert m1.getList().size() == 2 : "Unexpected number of props in the list 1";
        assert m2.getList().size() == 2 : "Unexpected number of props in the list 2";

        testSimple((PropertySimple) m1.getList().get(0), null, m1, "1");
        testSimple((PropertySimple) m1.getList().get(1), null, m1, "2");
        testSimple((PropertySimple) m2.getList().get(0), null, m2, "1");
        testSimple((PropertySimple) m2.getList().get(1), null, m2, "2");
    }

    public void testListOfMaps() {
        Configuration config = Configuration.builder().openList("l", "m").openMap().addSimple("c1", 1)
            .addSimple("c2", 2).closeMap()
            .openMap().addSimple("c1", 3).addSimple("c2", 4).closeMap().closeList().build();

        PropertyList l = config.getList("l");

        assert l != null : "Could not find top-level list";

        assert l.getList().size() == 2 : "Unexpected number of props in the top level list";

        PropertyMap m1 = (PropertyMap) l.getList().get(0);
        PropertyMap m2 = (PropertyMap) l.getList().get(1);

        assert m1.getParentList() == l : "Parent list on m1 not set";
        assert m1.getParentMap() == null : "Unexpected parent map on m1";
        assert m2.getParentList() == l : "Parent list on m2 not set";
        assert m1.getParentMap() == null : "Unexpected parent map on m2";

        assert m1.getMap().size() == 2 : "Unexpected number of props in the list 1";
        assert m2.getMap().size() == 2 : "Unexpected number of props in the list 2";

        testSimple(m1.getSimple("c1"), m1, null, "1");
        testSimple(m1.getSimple("c2"), m1, null, "2");
        testSimple(m2.getSimple("c1"), m2, null, "3");
        testSimple(m2.getSimple("c2"), m2, null, "4");
    }

    public void testMapOfMaps() {
        Configuration config = Configuration.builder().openMap("m").openMap("im1").addSimple("c1", 1).addSimple("c2", 2)
            .closeMap().openMap("im2").addSimple("c1", 3).addSimple("c2", 4).closeMap().closeMap().build();

        PropertyMap m = config.getMap("m");

        assert m != null : "Could not find the top level map";

        assert m.getMap().size() == 2 : "Unexpected number of props in the top level map";

        PropertyMap m1 = m.getMap("im1");
        PropertyMap m2 = m.getMap("im2");

        assert m1 != null : "Could not find im1";
        assert m2 != null : "Could not find im2";

        assert m1.getParentList() == null : "Unexpected parent list on m1";
        assert m1.getParentMap() == m : "Unexpected parent map on m1";
        assert m2.getParentList() == null : "Unexpected parent list on m2";
        assert m1.getParentMap() == m : "Unexpected parent map on m2";

        testSimple(m1.getSimple("c1"), m1, null, "1");
        testSimple(m1.getSimple("c2"), m1, null, "2");
        testSimple(m2.getSimple("c1"), m2, null, "3");
        testSimple(m2.getSimple("c2"), m2, null, "4");
    }

    public void testMapOfLists() {
        Configuration config = Configuration.builder().openMap("m").openList("il1", "m").addSimples(1, 2)
            .closeList().openList("il2", "m").addSimples(3, 4).closeList().closeMap().build();

        PropertyMap m = config.getMap("m");

        assert m != null : "Could not find the top level map";

        assert m.getMap().size() == 2 : "Unexpected number of props in the top level map";

        PropertyList l1 = m.getList("il1");
        PropertyList l2 = m.getList("il2");

        assert l1 != null : "Could not find il1";
        assert l2 != null : "Could not find il2";

        assert l1.getParentList() == null : "Unexpected parent list on l1";
        assert l1.getParentMap() == m : "Unexpected parent map on l1";
        assert l2.getParentList() == null : "Unexpected parent list on l2";
        assert l2.getParentMap() == m : "Unexpected parent map on l2";

        testSimple((PropertySimple) l1.getList().get(0), null, l1, "1");
        testSimple((PropertySimple) l1.getList().get(1), null, l1, "2");
        testSimple((PropertySimple) l2.getList().get(0), null, l2, "3");
        testSimple((PropertySimple) l2.getList().get(1), null, l2, "4");
    }

    public void testUtterMess() {
        Configuration config = Configuration.builder()
            .openList("l", "m") //
            /**/.openMap() //
            /**//**/.openMap("innerMap") //
            /**//**//**/.addSimple("c1", 1) //
            /**//**//**/.openList("c2", "m") //
            /**//**//**/.closeList() //
            /**//**/.closeMap() //
            /**//**/.addSimple("simple", 2) //
            /**//**/.openList("innerList", "m") //
            /**//**/.closeList() //
            /**/.closeMap() //
            /**/.addSimple(3) //
            /**/.openList("im") //
            /**//**/.openList("iim") //
            /**//**/.closeList() //
            /**//**/.openMap() //
            /**//**/.closeMap() //
            /**/.closeList() //
            .closeList().build();

        PropertyList l = config.getList("l");
        PropertyMap lm = (PropertyMap) l.getList().get(0);
        PropertyMap innerMap = lm.getMap("innerMap");
        PropertySimple c1 = innerMap.getSimple("c1");
        PropertyList c2 = innerMap.getList("c2");
        PropertySimple simple = lm.getSimple("simple");
        PropertyList innerList = lm.getList("innerList");
        PropertySimple ls = (PropertySimple) l.getList().get(1);
        PropertyList ll = (PropertyList) l.getList().get(2);
        PropertyList lll = (PropertyList) ll.getList().get(0);
        PropertyMap llm = (PropertyMap) ll.getList().get(1);

        //all the aspects of the above mess have been tested in the previous tests
        //this just really is here to prove the point of how messy our configs can be

        assert c1 != null;
        assert c2 != null;
        assert simple != null;
        assert innerList != null;
        assert ls != null;
        assert lll != null;
        assert llm != null;
    }

    public void testRawConfigs() {
        Configuration config = Configuration.builder().openRawConfiguration().withPath("a/b")
            .withContents("asdf", "123").closeRawConfiguration().build();

        RawConfiguration r = config.getRawConfigurations().iterator().next();
        assert r.getPath().equals("a/b") : "Unexpected path";
        assert r.getContents().equals("asdf") : "Unexpected contents";
        assert r.getSha256().equals("123") : "Unexpected sha256";
        assert r.getConfiguration() == config : "Unexpected raw config owning configuration";
    }

    private void testSimple(PropertySimple p, PropertyMap expectedParentMap, PropertyList expectedParentList,
        String expectedValue) {
        assertSame(p.getParentMap(), expectedParentMap, "Unexpected parent map");
        assertSame(p.getParentList(), expectedParentList, "Unexpected parent list");
        assertEquals(p.getStringValue(), expectedValue, "Unexpected value");
    }
}

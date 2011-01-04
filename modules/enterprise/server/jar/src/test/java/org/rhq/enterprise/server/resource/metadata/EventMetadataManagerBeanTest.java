package org.rhq.enterprise.server.resource.metadata;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;

import org.rhq.core.domain.event.EventDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;

public class EventMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = {"plugin.metadata", "Events.NewPlugin"})
    public void registerEventsPlugin() throws Exception {
        createPlugin("event-test-plugin", "1.0", "plugin_v1.xml");
    }

    @Test(groups = {"plugin.metadata", "Events.NewPlugin"}, dependsOnMethods = {"registerEventsPlugin"})
    public void persistNewEventDefs() throws Exception {
        assertResourceTypeAssociationEquals(
            "EventServer1",
            "EventMetadataManagerBeanTestPlugin",
            "eventDefinitions",
            asList("event1", "event2")
        );
    }

    @Test(groups = {"plugin.metadata", "Events.NewPlugin"}, dependsOnMethods = {"persistNewEventDefs"})
    public void persistNewEventDefProperties() {
        EventDefinition eventDef = loadEventDef("event1", "EventServer1");

        assertEquals("Failed to set EventDefinition.name", "event1", eventDef.getName());
        assertEquals("Failed to set EventDefinition.description", "Event 1", eventDef.getDescription());
    }

    @Test(groups = {"plugin.metadata", "Events.UpgradePlugin"}, dependsOnGroups = {"Events.NewPlugin"})
    public void upgradeEventsPlugin() throws Exception {
        createPlugin("event-test-plugin", "2.0", "plugin_v2.xml");
    }

    @Test(groups = {"plugin.metadata", "Events.UpgradePlugin"}, dependsOnMethods = {"upgradeEventsPlugin"})
    public void retainEventDefsOfTypeThatIsNotChangedDuringUpgrade() throws Exception {
        assertResourceTypeAssociationEquals(
            "EventServer1",
            "EventMetadataManagerBeanTestPlugin",
            "eventDefinitions",
            asList("event1", "event2")
        );
    }

    @Test(groups = {"plugin.metadata", "Events.UpgradePlugin"}, dependsOnMethods = {"upgradeEventsPlugin"})
    public void addNewEventDefs() throws Exception {
        assertResourceTypeAssociationEquals(
            "EventServer2",
            "EventMetadataManagerBeanTestPlugin",
            "eventDefinitions",
            asList("event1", "event2")
        );
    }

    @Test(groups = {"plugin.metadata", "Events.UpgradePlugin"}, dependsOnMethods = {"upgradeEventsPlugin"})
    public void deleteEventDefsThatHaveBeenRemovedInUpgradedType() throws Exception {
        assertResourceTypeAssociationEquals(
            "EventServer3",
            "EventMetadataManagerBeanTestPlugin",
            "eventDefinitions",
            EMPTY_LIST
        );
  }

    @Test(groups = {"plugin.metadata", "Events.UpgradePlugin"}, dependsOnMethods = {"upgradeEventsPlugin"})
    public void updateExistingEventDefs() throws Exception {
        assertResourceTypeAssociationEquals(
            "EventServer4",
            "EventMetadataManagerBeanTestPlugin",
            "eventDefinitions",
            asList("event1", "event3")
        );

        EventDefinition eventDef = loadEventDef("event1", "EventServer4");
        assertEquals("The description property should have been updated", "EVENT ONE", eventDef.getDescription());
    }

    EventDefinition loadEventDef(String name, String resourceType) {
        return  (EventDefinition) getEntityManager().createQuery(
            "from  EventDefinition e where e.name = :name and e.resourceType.name = :resourceType")
            .setParameter("name", name)
            .setParameter("resourceType", resourceType)
            .getSingleResult();
    }

}

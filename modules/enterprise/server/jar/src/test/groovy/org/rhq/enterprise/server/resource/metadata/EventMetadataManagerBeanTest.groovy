package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test
import org.rhq.core.domain.event.EventDefinition
import org.rhq.test.AssertUtils

class EventMetadataManagerBeanTest extends MetadataTest {

  @Test(groups = ['plugin.metadata', 'Events.NewPlugin'])
  void registerEventsPlugin() {
    def pluginDescriptor =
    """
    <plugin name="EventMetadataManagerBeanTestPlugin"
            displayName="MeasurementMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="EventServer1">
        <event name="event1" description="Event 1"/>
        <event name="event2" description="Event 2"/>
      </server>

      <server name="EventServer2"/>

      <server name="EventServer3">
        <event name="event1" description="Event 1"/>
        <event name="event2" description="Event 2"/>
      </server>

      <server name="EventServer4">
        <event name="event1" description="Event 1"/>
        <event name="event2" description="Event 2"/>
      </server>
    </plugin>
    """

    createPlugin("event-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['plugin.metadata', 'Events.NewPlugin'], dependsOnMethods = ['registerEventsPlugin'])
  void persistNewEventDefs() {
    assertResourceTypeAssociationEquals(
        'EventServer1',
        'EventMetadataManagerBeanTestPlugin',
        'eventDefinitions',
        ['event1', 'event2']
    )
  }

  @Test(groups = ['plugin.metadata', 'Events.NewPlugin'], dependsOnMethods = ['persistNewEventDefs'])
  void persistNewEventDefProperties() {
    def eventDef = loadEventDef('event1', 'EventServer1')

    assertEquals "Failed to set EventDefinition.name", 'event1', eventDef.name
    assertEquals "Failed to set EventDefinition.description", 'Event 1', eventDef.description
  }

  @Test(groups = ['plugin.metadata', 'Events.UpgradePlugin'], dependsOnGroups = ['Events.NewPlugin'])
  void upgradeEventsPlugin() {
    def pluginDescriptor =
    """
    <plugin name="EventMetadataManagerBeanTestPlugin"
            displayName="ContentMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="EventServer1">
        <event name="event1" description="Event 1"/>
        <event name="event2" description="Event 2"/>
      </server>

      <server name="EventServer2">
        <event name="event1" description="Event 1"/>
        <event name="event2" description="Event 2"/>
      </server>

      <server name="EventServer3"/>

      <server name="EventServer4">
        <event name="event1" description="EVENT ONE"/>
        <event name="event3" description="Event 3"/>
      </server>
    </plugin>
    """

    createPlugin("event-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['plugin.metadata', 'Events.UpgradePlugin'], dependsOnMethods = ['upgradeEventsPlugin'])
  void retainEventDefsOfTypeThatIsNotChangedDuringUpgrade() {
    assertResourceTypeAssociationEquals(
        'EventServer1',
        'EventMetadataManagerBeanTestPlugin',
        'eventDefinitions',
        ['event1', 'event2']
    )
  }

  @Test(groups = ['plugin.metadata', 'Events.UpgradePlugin'], dependsOnMethods = ['upgradeEventsPlugin'])
  void addNewEventDefs() {
    assertResourceTypeAssociationEquals(
        'EventServer2',
        'EventMetadataManagerBeanTestPlugin',
        'eventDefinitions',
        ['event1', 'event2']
    )
  }

  @Test(groups = ['plugin.metadata', 'Events.UpgradePlugin'], dependsOnMethods = ['upgradeEventsPlugin'])
  void deleteEventDefsThatHaveBeenRemovedInUpgradedType() {
    assertResourceTypeAssociationEquals(
        'EventServer3',
        'EventMetadataManagerBeanTestPlugin',
        'eventDefinitions',
        []
    )
  }

  @Test(groups = ['plugin.metadata', 'Events.UpgradePlugin'], dependsOnMethods = ['upgradeEventsPlugin'])
  void updateExistingEventDefs() {
    assertResourceTypeAssociationEquals(
        'EventServer4',
        'EventMetadataManagerBeanTestPlugin',
        'eventDefinitions',
        ['event1', 'event3']
    )

    def eventDef = loadEventDef('event1', 'EventServer4')
    assertEquals "The description property should have been updated", 'EVENT ONE', eventDef.description
  }

  EventDefinition loadEventDef(String name, String resourceType) {
    return  (EventDefinition) entityManager.createQuery(
    """
    from  EventDefinition e
    where e.name = :name and e.resourceType.name = :resourceType
    """
    ).setParameter('name', name)
     .setParameter('resourceType', resourceType)
     .getSingleResult()
  }

}

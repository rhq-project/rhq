package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test

import static org.rhq.core.domain.util.PageOrdering.ASC
import static org.rhq.core.domain.util.PageOrdering.DESC
import static org.testng.Assert.*
import org.testng.annotations.BeforeMethod

class SortDelegateTest {

  SortDelegate sort

  @BeforeMethod
  void setup() {
    sort = new SortDelegate()
  }

  @Test
  void addPropertyWithDefaultOrder() {
    sort.username

    assertSortFieldsEquals(
        sort.sortFields,
        [new SortField(name: 'username', order: ASC)],
        "Expected sortFields to contain <username> with default order of $ASC"
    )
  }

  @Test
  void addPropertyWithOrderSpecified() {
    sort.username.desc

    assertSortFieldsEquals(
        sort.sortFields,
        [new SortField(name: 'username', order: DESC)],
        "Expected sortFields to contain <username> with sort order of $DESC"            
    )
  }

  @Test
  void addMultipleProperties() {
    sort.username
    sort.id

    assertSortFieldsEquals(
        sort.sortFields,
        [new SortField(name: 'username', order: ASC), new SortField(name: 'id', order: ASC)],
        "Failed to specify multiple sort fields with implied ordering"
    )
  }

  @Test
  void addMultiplePropertiesWithOrderSpecified() {
    sort.username.desc
    sort.id.asc

    assertSortFieldsEquals(
        sort.sortFields,
        [new SortField(name: 'username', order: DESC), new SortField(name: 'id', order: ASC)],
        "Failed to specify multiple sort fields with order specified for each field"
    )
  }

  @Test
  void addMultiplePropertiesWithOrderSpecifiedForSome() {
    sort.username.desc
    sort.id

    assertSortFieldsEquals(
        sort.sortFields,
        [new SortField(name: 'username', order: DESC), new SortField(name: 'id', order: ASC)],
        "Failed to specify multiple sort fields with order specified for some of them"
    )
  }

  def assertSortFieldsEquals(List actual, List expected, String msg) {
    assertEquals(actual.size(), expected.size(), "$msg -- Lists do not have the same number of elements")

    def index = 0
    expected.each { expectedSortField ->
      def actualSortField = actual[index++]
      assertEquals(actualSortField.name, expectedSortField.name, "$msg -- sort fields differ, actual: $actual, expected: $expected")
      assertEquals(actualSortField.order, expectedSortField.order, "$msg -- sort fields differ, actual: $actual, expected: $expected")
    }
  }

}

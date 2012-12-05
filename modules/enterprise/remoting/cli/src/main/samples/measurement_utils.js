/**
 * Some functions to work with metrics.
 * Need util.js to be loaded/present
 *
 * @author John Sanda
 * @see http://johnsanda.blogspot.com/2010/09/updating-metric-collection-schedules.html
 */

var mm = new MeasurementModule();

testUpdates = {
  context: 'Resource',
  id:       10033,
  schedules: {
    'Late Collections':                mm.interval(15, mm.time.minutes),
    'Failed Collections per Minute':   mm.interval(15, mm.time.minutes),
    'Currently Schedule Measurements': 'enabled'
  }
}

testGroupUpdates = {
  context: 'Group',
  id:      10031,
  schedules: {
    'Used Swap Space': 'disabled',
    'Total Memory':    mm.interval(3, mm.time.minutes)
  }
}

/**
 * MeasurementModule is a class that provides properties and methods for working with
 * measurements.
 */
function MeasurementModule() {

  /**
   * Performs the actual updates, calling MeasurementScheduleManager.
   *
   * @param updates
   * @param criteria
   * @param enableSchedules
   * @param disableSchedules
   * @param updateSchedules
   */
  function doScheduleUpdates(updates, criteria, enableSchedules, disableSchedules, updateSchedules) {
    criteria.fetchDefinition(true);
    var schedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);

    foreach(schedules, function (schedule) {
      var measurementName = schedule.definition.displayName;

      if (updates.schedules[measurementName]) {
        switch (updates.schedules[measurementName]) {
          case 'enabled':
            MeasurementScheduleManager[enableSchedules](updates.id, [schedule.definition.id]);
            break;
          case 'disabled':
            MeasurementScheduleManager[disableSchedules](updates.id,
                [schedule.definition.id]);
            break;
          default:
            var interval = updates.schedules[measurementName];
            MeasurementScheduleManager[updateSchedules](updates.id,
                [schedule.definition.id], interval);
        }
      }
    });

  }

  /**
   * Intended for use with the interval method.
   */
  this.time = {
    seconds: 1000,
    minutes: 60 * 1000,
    hours:   60 * 60 * 1000
  }

  /**
   * A helper method that calculates a schedule's interval in milliseconds
   * @param num
   * @param time
   */
  this.interval = function (num, time) {
    return num * time;
  }

  /**
   * Updates the metric schedule as specified in the updates object. The object is expected
   * to contain three properties or keys. The first of these required keys is <context>.
   * Accepted values are the strings 'Resource' or 'Group'.
   *
   * The next required key is <id>, and its values is expected to be an integer. The
   * interpretation of its values is dependent on the value of <context>. When the value of
   * <context> is 'Resource', then <id> is treated as a resource id. If the value of
   * <context> is 'Group', then <id> is treated as a compatible group id.
   *
   * The third required key is <schedules>, and it a nested object that specifies the
   * schedules to be updated. The keys of <schedules> are the measurement display names.
   * Expected values are 'enabled', 'disabled', or an integer which specifies the
   * collection interval in milliseconds.
   *
   * Here is an example to illustrate what the updates object should look like:
   *
   *   resourceSchedulesUpdates = {
   *     context: 'Resource',
   *     id:      123,
   *     schedules: {
   *       'Measurement A': 'enabled',
   *       'Measurement B': 'disabled,
   *       'Measurement C': interval(20, time.minutes)
   *     }
   *   }
   *
   * @param updates The updates to perform
   */
  this.updateSchedules = function (updates) {
    if (!updates.id) {
      throw '<id> is a required property';
    }

    if (!updates.schedules) {
      throw '<schedules> is a required property';
    }

    if (updates.context == 'Resource') {
      var criteria = MeasurementScheduleCriteria();
      criteria.addFilterResourceId(updates.id);
      doScheduleUpdates(updates, criteria, 'enableSchedulesForResource',
          'disableSchedulesForResource', 'updateSchedulesForResource');
    }
    else if (updates.context == 'Group') {
      var criteria = MeasurementScheduleCriteria();
      criteria.addFilterResourceGroupId(updates.id);
      doScheduleUpdates(updates, criteria, 'enableSchedulesForCompatibleGroup',
          'disableSchedulesForCompatibleGroup', 'updateSchedulesForCompatibleGroup');
    }
    else {
      throw "Unrecognized value for context: " + updates.context + " - expected either " +
          "<Resource> or <Group>";
    }
  }
}

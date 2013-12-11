/**
 * This module provides functionality to generate storage cluster sizing estimates. The end goal is for it to support
 * generating estimates for both existing and new installations. For existing installations, the script will query
 * the existing RHQ database to get the necessary metric schedule info needed to generate the estimates. For new
 * installs, some input, in terms of the numbers and types of resources, will have to be provided in order to generate
 * the estimates. Right now the module has functionality for generating the sizes of the data and partition index files
 * for the metrics tables (which does not include the metrics_index table).
 *
 * current usage (from CLI shell):
 *
 * $ storage = require('modules:/rhq.storage.sizing.js');
 * $ results = storage.sizeOfRawMetrics({30000: 5, 60000: 5});
 */
(function() {
  var util = require('modules:/util');

  function Time() {
    this.second = 1000;
    this.minute = this.second * 60;
    this.hour = this.minute * 60;
    this.day = this.hour * 24;
    this.week = this.day * 7;
  }

  var time = new Time();

  /**
   * Encapsulates sizing data for a set of rows, having the samae number of columns and
   * each column being the same size.
   */
  function RowInfo() {
    // The total number of rows described by this RowInfo object
    this.numRows = 0;
    // The total number of columns contained in each row
    this.numColumns = 0;
    // The size of an individual row
    this.rowSize = 0;
    // The total size of all rows described by this RowInfo object
    this.totalSize = 0;
  }

  /**
   * Encapsulates sizing data for an RHQ table, e.g., raw_metrics, one_hour_metrics
   */
  function Table(name) {
    // The name of the table
    this.name = name;
    // The size of the partition index component
    this.partitionIndex = 0;
    // A Data object that corresponds to the Data component
    this.data = null;
  }

  /**
   * Encapsulates sizing info for the Data component
   */
  function Data() {
    // A map of RowInfo objects. Keys are collection intervals while values are the RowInfo
    // objects. This mapping allows us to see what footprint on disk a particular collection
    // interval will have.
    this.rows = {};
    // The overall total size of the Data component.
    this.totalSize = 0;
  }

  /**
   * In order to calculate the data component size, we need to know the number of rows and
   * columns. The number of rows is the number of schedules. The number of columns is
   * determined by the collection interval. Intermediate (per-row) results are grouped by
   * collection interval.
   *
   * @param schedules
   * @param columnSize
   * @param rowKeySize
   * @param duration
   * @returns data
   */
  var calculateData = function(schedules, columnSize, rowKeySize, duration) {
    var data = new Data();

    util.foreach(schedules, function(interval, numSchedules) {
      var rowInfo = new RowInfo();
      rowInfo.numColumns = duration / interval;
      rowInfo.rowSize = (rowInfo.numColumns * columnSize) + rowKeySize;
      rowInfo.numRows = numSchedules;
      rowInfo.totalSize = rowInfo.rowSize * rowInfo.numRows;

      data.rows[interval] = rowInfo;
      data.totalSize = data.totalSize + rowInfo.totalSize;
    });

    return data;
  };

  var calculatePartitionIndex = function(keySize, rowSize, columnNameSize, numRows) {
    var keyLength = 2;
    var position = 8;
    var promotedSize = 4;

    if (rowSize < 65536) {
      return (keyLength + keySize + position + promotedSize) * numRows;
    }

    var localDeletionTime = 4;
    var markedForDeleteAt = 8;
    var columnIndexSize =4;

    // compute index entry size
    var firstNameLen = 2;
    var firstName = columnNameSize;
    var lastNameLen = 2;
    var lastName = columnNameSize;
    var offset = 8;
    var width = 8;
    var columnIndexEntry = firstNameLen + firstName + lastNameLen + lastName + offset + width;

    var numColumnIndexEntries = Math.ceil(rowSize / 65536);

    return (keyLength + keySize + position + promotedSize + localDeletionTime + markedForDeleteAt + columnIndexSize +
        (numColumnIndexEntries * columnIndexEntry)) * numRows;
  };

  var calculateMetricsTableSize = function(name, columnSize, rowKeySize, rowKeyValueSize, columnNameSize, duration,
    schedules) {
    var table = new Table(name);
    table.data = calculateData(schedules, columnSize, rowKeySize, duration);

    util.foreach(table.data.rows, function(interval) {
      var rowInfo = table.data.rows[interval];
      table.partitionIndex += calculatePartitionIndex(rowKeyValueSize, rowInfo.rowSize, columnNameSize, rowInfo.numRows);
    });

    return table;
  };

  var calculateAggregatesTableSize = function(name, duration, schedules) {
    // Here is the break down the of the column size for aggregate metrics tables
    //
    // date component - 11         (see org.apache.cassandra.db.marshal.CompositeType for more details on byte encoding
    // type component - 7           byte encoding of composite types. In short, there are an extran 3 bytes of overhead
    // column name length - 2       per component.)
    // flags - 1
    // TTL - 4
    // deletion time - 4
    // timestamp - 8
    // column value length - 4
    // column value - 8
    var columnSize = 49;

    // See the rowKeySize for rawMetrics. The byte-wise break down is the same.
    var rowKeySize = 30;

    var columnNameSize = 18;
    var rowKeyValueSize = 4;

    // We have to multiply the column size by 3 because there are 3 values (i.e., columns)
    // per aggregate metric.
    return calculateMetricsTableSize(name, columnSize * 3, rowKeySize, rowKeyValueSize, columnNameSize, duration,
        schedules);
  };

  exports.loadSchedules = function() {
    var criteria = MeasurementScheduleCriteria();
    criteria.addFilterEnabled(true);
    criteria.addSortId(PageOrdering.ASC);
    criteria.setPaging(0, 500);

    var schedulesSummary = {};

    util.foreach(criteria, function(schedule) {
      var count = schedulesSummary[schedule.interval];
      if (count == null) {
        schedulesSummary[schedule.interval] = 1;
      } else {
        schedulesSummary[schedule.interval] = count + 1;
      }
    });

    return schedulesSummary;
  };

  exports.time = new Time();

  /**
   * Calculates SSTable component files sizes for the raw_metrics table for a set of schedules.
   *
   * @param schedules A map of collection intervals to the number of schedules for each
   * interval. It is assumed that counts include only enabled schedules.
   *
   * @returns Table
   */
  exports.sizeOfRawMetrics = function(schedules) {
    // Here is a byte-wise break down for the overall column size
    //
    // column name length - 2
    // column name - 8
    // flags - 1
    // TTL - 4
    // local deletion time - 4
    // timestmap - 8
    // column value length - 8
    // column value - 8
    var columnSize = 39;

    //Here is a byte-wise break down for the row key. Note that this is the total row key
    // overhead, not just the length of the key itself.
    //
    // key length - 2
    // key value - 4
    // columns size - 8
    // local deletion time - 4
    // marked for delete at - 8
    // column count - 4
    var rowKeySize = 30;

    var rowKeyValueSize = 4;
    var columnNameSize = 8;

    return calculateMetricsTableSize('raw_metrics', columnSize, rowKeySize, rowKeyValueSize, columnNameSize, time.week,
        schedules);
  };

  /**
   * Calculates SSTable component files sizes for the one_hour_metrics table for a set of schedules. Note that this
   * method is exposed right now primarily for testing. Moreover, it is assumed that the schedules argument is not the
   * same as the one that would be passed to the sizeOfRawMetrics function.
   *
   * @param schedules A map of collection intervals to the number of schedules for each
   * interval. It is assumed that counts include only enabled schedules.
   *
   * @returns Table
   */
  exports.sizeOf1HourMetrics = function(schedules) {
    return calculateAggregatesTableSize('one_hour_metrics', time.day * 7, schedules);
  };

  /**
   * Calculates SSTable component files sizes for the six_hour_metrics table for a set of schedules. Note that this
   * method is exposed right now primarily for testing. Moreover, it is assumed that the schedules argument is not the
   * same as the one that would be passed to the sizeOfRawMetrics function.
   *
   * @param schedules A map of collection intervals to the number of schedules for each
   * interval. It is assumed that counts include only enabled schedules.
   *
   * @returns Table
   */
  exports.sizeOf6HourMetrics = function(schedules) {
    return calculateAggregatesTableSize('six_hour_metrics', time.day * 31, schedules);
  };

  /**
   * Calculates SSTable component files sizes for the twenty_four_hour_metrics table for a set of schedules. Note that
   * this method is exposed right now primarily for testing. Moreover, it is assumed that the schedules argument is not
   * the same as the one that would be passed to the sizeOfRawMetrics function.
   *
   * @param schedules A map of collection intervals to the number of schedules for each
   * interval. It is assumed that counts include only enabled schedules.
   *
   * @returns Table
   */
  exports.sizeOf24HourMetrics = function(schedules) {
    return calculateAggregatesTableSize('twenty_four_hour_metrics', time.day * 365, schedules);
  };
})();
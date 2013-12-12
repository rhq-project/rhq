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

  function BloomFilter(keys) {
    function BloomSpecification(k, bucketsPerElement) {
      this.K = k;
      this.bucketsPerElement = bucketsPerElement;
    }

    // This is taken directly from BloomCalculations.java
    this.probs = [
      [1.0], // dummy row representing 0 buckets per element
      [1.0, 1.0], // dummy row representing 1 buckets per element
      [1.0, 0.393,  0.400],
      [1.0, 0.283,  0.237,   0.253],
      [1.0, 0.221,  0.155,   0.147,   0.160],
      [1.0, 0.181,  0.109,   0.092,   0.092,   0.101], // 5
      [1.0, 0.154,  0.0804,  0.0609,  0.0561,  0.0578,   0.0638],
      [1.0, 0.133,  0.0618,  0.0423,  0.0359,  0.0347,   0.0364],
      [1.0, 0.118,  0.0489,  0.0306,  0.024,   0.0217,   0.0216,   0.0229],
      [1.0, 0.105,  0.0397,  0.0228,  0.0166,  0.0141,   0.0133,   0.0135,   0.0145],
      [1.0, 0.0952, 0.0329,  0.0174,  0.0118,  0.00943,  0.00844,  0.00819,  0.00846], // 10
      [1.0, 0.0869, 0.0276,  0.0136,  0.00864, 0.0065,   0.00552,  0.00513,  0.00509],
      [1.0, 0.08,   0.0236,  0.0108,  0.00646, 0.00459,  0.00371,  0.00329,  0.00314],
      [1.0, 0.074,  0.0203,  0.00875, 0.00492, 0.00332,  0.00255,  0.00217,  0.00199,  0.00194],
      [1.0, 0.0689, 0.0177,  0.00718, 0.00381, 0.00244,  0.00179,  0.00146,  0.00129,  0.00121,  0.0012],
      [1.0, 0.0645, 0.0156,  0.00596, 0.003,   0.00183,  0.00128,  0.001,    0.000852, 0.000775, 0.000744], // 15
      [1.0, 0.0606, 0.0138,  0.005,   0.00239, 0.00139,  0.000935, 0.000702, 0.000574, 0.000505, 0.00047,  0.000459],
      [1.0, 0.0571, 0.0123,  0.00423, 0.00193, 0.00107,  0.000692, 0.000499, 0.000394, 0.000335, 0.000302, 0.000287, 0.000284],
      [1.0, 0.054,  0.0111,  0.00362, 0.00158, 0.000839, 0.000519, 0.00036,  0.000275, 0.000226, 0.000198, 0.000183, 0.000176],
      [1.0, 0.0513, 0.00998, 0.00312, 0.0013,  0.000663, 0.000394, 0.000264, 0.000194, 0.000155, 0.000132, 0.000118, 0.000111, 0.000109],
      [1.0, 0.0488, 0.00906, 0.0027,  0.00108, 0.00053,  0.000303, 0.000196, 0.00014,  0.000108, 8.89e-05, 7.77e-05, 7.12e-05, 6.79e-05, 6.71e-05] // 20
    ]; // the first column is a dummy column representing K=0.

    var self = this;
    var excess = 20;
    var bitset_excess = 20;
    var minBuckets = 2;
    var minK = 1;
    var optKPerBuckets = [];
    var maxFalsePosProb = 0.01;


    for (i = 0; i < this.probs.length; i++) {
      var min = java.lang.Double.MAX_VALUE;
      var prob = this.probs[i];
      for (j = 0; j < prob.length; j++) {
        if (prob[j] < min) {
          min = prob[j];
          optKPerBuckets[i] = Math.max(minK, j);
        }
      }
    }

    var bucketsPerElement = maxBucketsPerElement(keys);
    var spec = computeBloomSpec(bucketsPerElement, maxFalsePosProb);
    var numBits = (keys * spec.bucketsPerElement) + bitset_excess;
    var wordCount = bits2words(numBits);

    if (wordCount > java.lang.Integer.MAX_VALUE) {
      throw "Bloom filter size is > 16GB, reduce the bloom_filter_fp_chance";
    }

    var bytes = wordCount * 8;
    this.size = bytes + 8;

    function maxBucketsPerElement(numElements) {
      numElements = Math.max(1, numElements);
      var v = (java.lang.Long.MAX_VALUE - excess) / keys;
      if (v < 1) {
        throw "Cannot compute probabilities for " + numElements + " elements.";
      }
      return Math.min(self.probs.length - 1, v);
    }

    // This is taken from BloomCalculations.java
    function computeBloomSpec(maxBucketsPerElement, maxFalsePosProb) {
      var maxK = self.probs[maxBucketsPerElement].length - 1;

      // Handle the trivial cases
      if(maxFalsePosProb >= self.probs[minBuckets][minK]) {
        return new BloomSpecification(2, optKPerBuckets[2]);
      }
      if (maxFalsePosProb < self.probs[maxBucketsPerElement][maxK]) {
        throw "Unable to satisfy " + maxFalsePosProb + " with " + maxBucketsPerElement + " buckets per element";
      }

      // First find the minimal required number of buckets:
      var bucketsPerElement = 2;
      var K = optKPerBuckets[2];
      while(self.probs[bucketsPerElement][K] > maxFalsePosProb){
        bucketsPerElement++;
        K = optKPerBuckets[bucketsPerElement];
      }
      // Now that the number of buckets is sufficient, see if we can relax K
      // without losing too much precision.
      while(self.probs[bucketsPerElement][K - 1] <= maxFalsePosProb) {
        K--;
      }
      println('K = ' + K + ', bucketsPerElement = ' + bucketsPerElement);
      return new BloomSpecification(K, bucketsPerElement);
    }

    function bits2words(numBits) {
      return (((numBits-1)>>>6)+1);
    }
  }

  /**
   * Encapsulates sizing data for a set of rows, having the same number of columns and
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
    table.bloomFilter = new BloomFilter(5).size;

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

    return calculateMetricsTableSize('raw_metrics', columnSize, rowKeySize, rowKeyValueSize, columnNameSize,
        time.week, schedules);
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

  // exposed for testing
  exports.bloomFilter = function(keys) {
    return new BloomFilter(keys).size;
  }
})();
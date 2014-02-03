package org.rhq.server.metrics.aggregation;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.ListeningExecutorService;

import org.joda.time.DateTime;

import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.SignalingCountDownLatch;

/**
 * @author John Sanda
 */
class AggregationState {

    private DateTime startTime;

    private int batchSize;

    private MetricsDAO dao;

    private ListeningExecutorService aggregationTasks;

    private Semaphore permits;

    private SignalingCountDownLatch rawAggregationDone;

    private SignalingCountDownLatch oneHourAggregationDone;

    private SignalingCountDownLatch sixHourAggregationDone;

    private AtomicInteger remainingRawData;

    private AtomicInteger remaining1HourData;

    private AtomicInteger remaining6HourData;

    private DateTime oneHourTimeSlice;

    private DateTime oneHourTimeSliceEnd;

    private DateTime sixHourTimeSlice;

    private DateTime sixHourTimeSliceEnd;

    private DateTime twentyFourHourTimeSlice;

    private DateTime twentyFourHourTimeSliceEnd;

    private boolean sixHourTimeSliceFinished;

    private boolean twentyFourHourTimeSliceFinished;

    private Compute1HourData compute1HourData;

    private Compute6HourData compute6HourData;

    private Compute24HourData compute24HourData;

    DateTime getStartTime() {
        return startTime;
    }

    AggregationState setStartTime(DateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    int getBatchSize() {
        return batchSize;
    }

    AggregationState setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    MetricsDAO getDao() {
        return dao;
    }

    AggregationState setDao(MetricsDAO dao) {
        this.dao = dao;
        return this;
    }

    public ListeningExecutorService getAggregationTasks() {
        return aggregationTasks;
    }

    public AggregationState setAggregationTasks(ListeningExecutorService aggregationTasks) {
        this.aggregationTasks = aggregationTasks;
        return this;
    }

    Semaphore getPermits() {
        return permits;
    }

    AggregationState setPermits(Semaphore permits) {
        this.permits = permits;
        return this;
    }

    SignalingCountDownLatch getRawAggregationDone() {
        return rawAggregationDone;
    }

    AggregationState setRawAggregationDone(SignalingCountDownLatch rawAggregationDone) {
        this.rawAggregationDone = rawAggregationDone;
        return this;
    }

    SignalingCountDownLatch getOneHourAggregationDone() {
        return oneHourAggregationDone;
    }

    AggregationState setOneHourAggregationDone(SignalingCountDownLatch oneHourAggregationDone) {
        this.oneHourAggregationDone = oneHourAggregationDone;
        return this;
    }

    SignalingCountDownLatch getSixHourAggregationDone() {
        return sixHourAggregationDone;
    }

    AggregationState setSixHourAggregationDone(SignalingCountDownLatch sixHourAggregationDone) {
        this.sixHourAggregationDone = sixHourAggregationDone;
        return this;
    }

    /**
     * @return The remaining number of schedules with raw data to be aggregated
     */
    public AtomicInteger getRemainingRawData() {
        return remainingRawData;
    }

    public AggregationState setRemainingRawData(AtomicInteger remainingRawData) {
        this.remainingRawData = remainingRawData;
        return this;
    }

    /**
     * @return The remaining number of schedules with 1 hour data to be aggregated
     */
    public AtomicInteger getRemaining1HourData() {
        return remaining1HourData;
    }

    public AggregationState setRemaining1HourData(AtomicInteger remaining1HourData) {
        this.remaining1HourData = remaining1HourData;
        return this;
    }

    /**
     * @return The remaining number of schedules with 6 hour data to be aggregated
     */
    public AtomicInteger getRemaining6HourData() {
        return remaining6HourData;
    }

    public AggregationState setRemaining6HourData(AtomicInteger remaining6HourData) {
        this.remaining6HourData = remaining6HourData;
        return this;
    }

    public DateTime getOneHourTimeSlice() {
        return oneHourTimeSlice;
    }

    public AggregationState setOneHourTimeSlice(DateTime oneHourTimeSlice) {
        this.oneHourTimeSlice = oneHourTimeSlice;
        return this;
    }

    DateTime getOneHourTimeSliceEnd() {
        return oneHourTimeSliceEnd;
    }

    AggregationState setOneHourTimeSliceEnd(DateTime oneHourTimeSliceEnd) {
        this.oneHourTimeSliceEnd = oneHourTimeSliceEnd;
        return this;
    }

    public DateTime getSixHourTimeSlice() {
        return sixHourTimeSlice;
    }

    public AggregationState setSixHourTimeSlice(DateTime sixHourTimeSlice) {
        this.sixHourTimeSlice = sixHourTimeSlice;
        return this;
    }

    public DateTime getSixHourTimeSliceEnd() {
        return sixHourTimeSliceEnd;
    }

    public AggregationState setSixHourTimeSliceEnd(DateTime sixHourTimeSliceEnd) {
        this.sixHourTimeSliceEnd = sixHourTimeSliceEnd;
        return this;
    }

    public DateTime getTwentyFourHourTimeSlice() {
        return twentyFourHourTimeSlice;
    }

    public AggregationState setTwentyFourHourTimeSlice(DateTime twentyFourHourTimeSlice) {
        this.twentyFourHourTimeSlice = twentyFourHourTimeSlice;
        return this;
    }

    public DateTime getTwentyFourHourTimeSliceEnd() {
        return twentyFourHourTimeSliceEnd;
    }

    public AggregationState setTwentyFourHourTimeSliceEnd(DateTime twentyFourHourTimeSliceEnd) {
        this.twentyFourHourTimeSliceEnd = twentyFourHourTimeSliceEnd;
        return this;
    }

    public boolean is6HourTimeSliceFinished() {
        return sixHourTimeSliceFinished;
    }

    public AggregationState set6HourTimeSliceFinished(boolean is6HourTimeSliceFinished) {
        this.sixHourTimeSliceFinished = is6HourTimeSliceFinished;
        return this;
    }

    public boolean is24HourTimeSliceFinished() {
        return twentyFourHourTimeSliceFinished;
    }

    public AggregationState set24HourTimeSliceFinished(boolean is24HourTimeSliceFinished) {
        this.twentyFourHourTimeSliceFinished = is24HourTimeSliceFinished;
        return this;
    }

    public Compute1HourData getCompute1HourData() {
        return compute1HourData;
    }

    public AggregationState setCompute1HourData(Compute1HourData compute1HourData) {
        this.compute1HourData = compute1HourData;
        return this;
    }

    public Compute6HourData getCompute6HourData() {
        return compute6HourData;
    }

    public AggregationState setCompute6HourData(Compute6HourData compute6HourData) {
        this.compute6HourData = compute6HourData;
        return this;
    }

    public Compute24HourData getCompute24HourData() {
        return compute24HourData;
    }

    public AggregationState setCompute24HourData(Compute24HourData compute24HourData) {
        this.compute24HourData = compute24HourData;
        return this;
    }

}

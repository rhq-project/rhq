package org.rhq.server.metrics;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.util.concurrent.ListeningExecutorService;

import org.joda.time.DateTime;

/**
 * @author John Sanda
 */
public class AggregationState {

    private ListeningExecutorService aggregationTasks;

    private CountDownLatch rawIndexEntriesArrival;

    private SignalingCountDownLatch oneHourIndexEntriesArrival;

    private SignalingCountDownLatch sixHourIndexEntriesArrival;

    private AtomicInteger remainingRawData;

    private AtomicInteger remaining1HourData;

    private AtomicInteger remaining6HourData;

    private Set<Integer> oneHourIndexEntries;

    private Set<Integer> sixHourIndexEntries;

    private ReentrantReadWriteLock oneHourIndexEntriesLock;

    private ReentrantReadWriteLock sixHourIndexEntriesLock;

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

    public ListeningExecutorService getAggregationTasks() {
        return aggregationTasks;
    }

    public AggregationState setAggregationTasks(ListeningExecutorService aggregationTasks) {
        this.aggregationTasks = aggregationTasks;
        return this;
    }

    public CountDownLatch getRawIndexEntriesArrival() {
        return rawIndexEntriesArrival;
    }

    public AggregationState setRawIndexEntriesArrival(CountDownLatch rawIndexEntriesArrival) {
        this.rawIndexEntriesArrival = rawIndexEntriesArrival;
        return this;
    }

    public SignalingCountDownLatch getOneHourIndexEntriesArrival() {
        return oneHourIndexEntriesArrival;
    }

    public AggregationState setOneHourIndexEntriesArrival(SignalingCountDownLatch oneHourIndexEntriesArrival) {
        this.oneHourIndexEntriesArrival = oneHourIndexEntriesArrival;
        return this;
    }

    public SignalingCountDownLatch getSixHourIndexEntriesArrival() {
        return sixHourIndexEntriesArrival;
    }

    public AggregationState setSixHourIndexEntriesArrival(SignalingCountDownLatch sixHourIndexEntriesArrival) {
        this.sixHourIndexEntriesArrival = sixHourIndexEntriesArrival;
        return this;
    }

    public AtomicInteger getRemainingRawData() {
        return remainingRawData;
    }

    public AggregationState setRemainingRawData(AtomicInteger remainingRawData) {
        this.remainingRawData = remainingRawData;
        return this;
    }

    public AtomicInteger getRemaining1HourData() {
        return remaining1HourData;
    }

    public AggregationState setRemaining1HourData(AtomicInteger remaining1HourData) {
        this.remaining1HourData = remaining1HourData;
        return this;
    }

    public AtomicInteger getRemaining6HourData() {
        return remaining6HourData;
    }

    public AggregationState setRemaining6HourData(AtomicInteger remaining6HourData) {
        this.remaining6HourData = remaining6HourData;
        return this;
    }

    public Set<Integer> getOneHourIndexEntries() {
        return oneHourIndexEntries;
    }

    public AggregationState setOneHourIndexEntries(Set<Integer> oneHourIndexEntries) {
        this.oneHourIndexEntries = oneHourIndexEntries;
        return this;
    }

    public Set<Integer> getSixHourIndexEntries() {
        return sixHourIndexEntries;
    }

    public AggregationState setSixHourIndexEntries(Set<Integer> sixHourIndexEntries) {
        this.sixHourIndexEntries = sixHourIndexEntries;
        return this;
    }

    public ReentrantReadWriteLock getOneHourIndexEntriesLock() {
        return oneHourIndexEntriesLock;
    }

    public AggregationState setOneHourIndexEntriesLock(ReentrantReadWriteLock oneHourIndexEntriesLock) {
        this.oneHourIndexEntriesLock = oneHourIndexEntriesLock;
        return this;
    }

    public ReentrantReadWriteLock getSixHourIndexEntriesLock() {
        return sixHourIndexEntriesLock;
    }

    public AggregationState setSixHourIndexEntriesLock(ReentrantReadWriteLock sixHourIndexEntriesLock) {
        this.sixHourIndexEntriesLock = sixHourIndexEntriesLock;
        return this;
    }

    public DateTime getOneHourTimeSlice() {
        return oneHourTimeSlice;
    }

    public AggregationState setOneHourTimeSlice(DateTime oneHourTimeSlice) {
        this.oneHourTimeSlice = oneHourTimeSlice;
        return this;
    }

    public DateTime getOneHourTimeSliceEnd() {
        return oneHourTimeSliceEnd;
    }

    public AggregationState setOneHourTimeSliceEnd(DateTime oneHourTimeSliceEnd) {
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

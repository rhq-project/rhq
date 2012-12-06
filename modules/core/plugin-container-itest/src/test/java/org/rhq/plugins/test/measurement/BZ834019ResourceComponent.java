package org.rhq.plugins.test.measurement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

public class BZ834019ResourceComponent implements ResourceComponent<ResourceComponent<?>>, MeasurementFacet {

    public class CollectedMetric {
        public String metricName;
        public long collectedTime; // the collected time as relative to initialCollectionTime
        public long finishedTime; // the time when the getValues returned relative to initialCollectionTime
    }

    public CountDownLatch latch;
    public long initialCollectionTime = 0L;
    public Vector<CollectedMetric> collectedMetrics = new Vector<CollectedMetric>(); // we want synchronized Vector

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws Exception {
    }

    @Override
    public void stop() {
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        boolean doSleep = false;
        if (initialCollectionTime == 0L) {
            initialCollectionTime = System.currentTimeMillis();
            doSleep = true;
            latch = new CountDownLatch(1); // so the rest of our collections will wait for our sleep to finish
        }

        ArrayList<CollectedMetric> thisCollection = new ArrayList<CollectedMetric>();

        long now = System.currentTimeMillis() - initialCollectionTime;
        for (Iterator<MeasurementScheduleRequest> i = metrics.iterator(); i.hasNext();) {
            MeasurementScheduleRequest metric = i.next();
            log("collecting metric " + (metric.getName() + " (interval=" + metric.getInterval() + ")"));
            report.addData(new MeasurementDataNumeric(metric, new Double(1.0)));

            // remember what time we were called and which metric was asked to be collected
            CollectedMetric collectedMetric = new CollectedMetric();
            collectedMetric.collectedTime = now;
            collectedMetric.metricName = metric.getName();
            collectedMetrics.add(collectedMetric);
            thisCollection.add(collectedMetric);
        }

        // delay the initial collection a long time to push back collection of future metrics - we must ignore all interrupts!
        if (doSleep) {
            log("before sleep");
            for (int i = 0; i < 91; i++) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log("ignoring interrupt at second #" + i);
                }
            }
            log("after sleep");
            latch.countDown();
        } else {
            // don't let interrupts abort our wait - always wait for the countdown latch to open up
            boolean shouldWait = true;
            while (shouldWait) {
                try {
                    log("before latch wait");
                    latch.await();
                    log("latch opened up");
                    shouldWait = false;
                } catch (Exception e) {
                    log("latch wait interruptted, go back and wait...");
                }
            }
        }

        now = System.currentTimeMillis() - initialCollectionTime;
        for (CollectedMetric collection : thisCollection) {
            collection.finishedTime = now;
        }
        return;
    }

    private void log(String msg) {
        //System.out.println("!!!!!!!!!!! " + this.getClass().getSimpleName() + ": " + new Date() + ": " + msg);
    }
}
package org.rhq.server.metrics;

import java.util.concurrent.CountDownLatch;

/**
 * @author John Sanda
 */
public class SignalingCountDownLatch {

    private boolean aborted;

    private CountDownLatch latch;

    public SignalingCountDownLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void await() throws InterruptedException, AbortedException {
        latch.await();
        if (aborted) {
            throw new AbortedException();
        }
    }

    public void abort() {
        aborted = true;
        latch.countDown();
    }

    public void countDown() {
        latch.countDown();
    }

}

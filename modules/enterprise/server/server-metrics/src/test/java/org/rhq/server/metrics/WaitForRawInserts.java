package org.rhq.server.metrics;

import static org.testng.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Throwables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementDataNumeric;

/**
* @author John Sanda
*/
class WaitForRawInserts implements RawDataInsertedCallback {

    private final Log log = LogFactory.getLog(WaitForRawInserts.class);

    private CountDownLatch latch;

    private Throwable throwable;

    public WaitForRawInserts(int numInserts) {
        latch = new CountDownLatch(numInserts);
    }

    @Override
    public void onFinish() {
    }

    @Override
    public void onSuccess(MeasurementDataNumeric measurementDataNumeric) {
        latch.countDown();
    }

    @Override
    public void onFailure(Throwable throwable) {
        latch.countDown();
        this.throwable = throwable;
        log.error("An async operation failed", throwable);
    }

    public void await(String errorMsg) throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
        if (throwable != null) {
            fail(errorMsg, Throwables.getRootCause(throwable));
        }
    }
}

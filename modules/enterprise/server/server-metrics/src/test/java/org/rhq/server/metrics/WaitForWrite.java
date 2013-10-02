package org.rhq.server.metrics;

import static org.testng.Assert.fail;

import java.util.concurrent.CountDownLatch;

import com.datastax.driver.core.ResultSet;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* @author John Sanda
*/
class WaitForWrite implements FutureCallback<ResultSet> {

    private final Log log = LogFactory.getLog(WaitForWrite.class);

    private CountDownLatch latch;

    private Throwable throwable;

    public WaitForWrite(int numResults) {
        latch = new CountDownLatch(numResults);
    }

    @Override
    public void onSuccess(ResultSet rows) {
        latch.countDown();
    }

    @Override
    public void onFailure(Throwable throwable) {
        latch.countDown();
        this.throwable = throwable;
        log.error("An async operation failed", throwable);
    }

    public void await(String errorMsg) throws InterruptedException {
        latch.await();
        if (throwable != null) {
            fail(errorMsg, Throwables.getRootCause(throwable));
        }
    }

}

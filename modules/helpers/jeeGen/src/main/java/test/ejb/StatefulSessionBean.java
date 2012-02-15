package test.ejb;

import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;

/**
 * Stateful session bean - impl.
 */
public class StatefulSessionBean implements SessionBean, SessionSynchronization {

    private static final long serialVersionUID = 1L;

    private SessionContext context;

    private AtomicInteger newValue; // value inside Tx, not yet committed
    private int value;              // actual state of the bean
    
    public int increment() {
        return newValue.incrementAndGet();
    }

    public int decrement() {
        return newValue.decrementAndGet();
    }

    public void ejbCreate() {
        this.value = 0;
        this.newValue = new AtomicInteger(this.value);
    }

    public void ejbActivate() {
        return;
    }

    public void ejbPassivate() {
        return;
    }

    public void ejbRemove() {
        return;
    }
    
    public void setSessionContext(SessionContext context) {
        this.context = context;
    }

    @Override
    public void afterBegin() throws EJBException, RemoteException {
        return;
    }

    @Override
    public void beforeCompletion() throws EJBException, RemoteException {
        return;
    }

    @Override
    public void afterCompletion(boolean committed) throws EJBException, RemoteException {
        if (committed) {
            this.value = this.newValue.get();
        } else {
            this.newValue.set(this.value);
        }
    }

}
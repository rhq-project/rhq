package test.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * Stateful session bean - remote interface.
 */
public interface StatefulSessionEJBObject extends EJBObject {

    int increment() throws RemoteException;

    int decrement() throws RemoteException;

}

package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipRmi extends Remote {
    void join() throws RemoteException;

    void leave() throws RemoteException;
}

package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MembershipRmi extends Remote {
    String join() throws RemoteException;

    String leave() throws RemoteException;
}

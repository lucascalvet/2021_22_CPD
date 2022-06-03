package processors.client.membership;

import protocol.MembershipNode;
import protocol.Node;
import rmi.MembershipRmi;

import java.rmi.RemoteException;

public class ReconnectProcessor implements Runnable {
    private final Node node;
    private final MembershipNode membershipNode;

    public ReconnectProcessor(Node node, MembershipNode membershipNode) {
        this.node = node;
        this.membershipNode = membershipNode;
    }

    public void run() {
        new JoinProcessor(this.node, membershipNode).run();
        new LeaveProcessor(this.node, membershipNode).run();
    }
}

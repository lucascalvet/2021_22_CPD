package processors.client.membership;

import protocol.MembershipNode;
import protocol.Node;
import rmi.MembershipRmi;

import java.rmi.RemoteException;

public class RMIProcessor implements MembershipRmi {
    private final Node node;
    private final MembershipNode membershipNode;

    public RMIProcessor(Node node, MembershipNode membershipNode) {
        this.node = node;
        this.membershipNode = membershipNode;
    }

    @Override
    public String join() throws RemoteException {
        JoinProcessor joinProcessor = new JoinProcessor(this.node, membershipNode);
        joinProcessor.run();
        return joinProcessor.getResult();
    }

    @Override
    public String leave() throws RemoteException {
        LeaveProcessor leaveProcessor = new LeaveProcessor(this.node, membershipNode);
        leaveProcessor.run();
        return leaveProcessor.getResult();
    }
}

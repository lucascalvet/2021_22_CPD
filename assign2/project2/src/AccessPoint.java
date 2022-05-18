import java.net.InetAddress;
import java.net.UnknownHostException;

public class AccessPoint {
    private InetAddress ip;
    private Integer port;

    public AccessPoint(InetAddress ip, Integer port){
        this.ip = ip;
        this.port = port;
    }

    public AccessPoint(String apString) throws UnknownHostException {
        String[] apParts = apString.split(":");

        this.ip = InetAddress.getByName(apParts[0]);
        this.port = Integer.valueOf(apParts[1]);
    }

    public InetAddress getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;

        // null check
        if (o == null)
            return false;

        // type check and cast
        if (getClass() != o.getClass())
            return false;

        AccessPoint ap = (AccessPoint) o;

        // field comparison
        return ip.equals(ap.getIp()) && port.equals(ap.getPort());
    }
}

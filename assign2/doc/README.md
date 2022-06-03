# Proj. 2: Distributed and Partitioned Key-Value Store

## Store

### Compiling

### Running

```
java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>
```

- `<IP_mcast_addr>` is the address of the IP multicast group used by the membership service
- `<IP_mcast_port>` is the port number of the IP multicast group used by the membership service
- `<node_id>` is the node's id and it must be unique in a cluster (its IP address)
- `<Store_port>` is the port number used by the storage service 

## Test Client

### Compiling

### Running

```
java TestClient <node_ap> <operation> [<opnd>]
```

- `<node_ap>` is the node's access point. This depends on the implementation. If the service uses UDP or TCP, the format of the access point must be <IP address>:<port number>, where <IP address> and <port number> are respectively the IP address and the port number being used by the node. If the service uses RMI, this must be the IP address and the name of the remote object providing the service.
- <operation> is the string specifying the operation the node must execute. It can be either a key-value operation, i.e. "put", "get" or "delete", or a membership operation, i.e. "join" or "leave
- <opnd> is the argument of the operation. It is used only for key-value operations. In the case of:
    - put, it is the file pathname of the file with the value to add
    - otherwise (get or delete), it is the string of hexadecimal symbols encoding the sha-256 key returned by put. 



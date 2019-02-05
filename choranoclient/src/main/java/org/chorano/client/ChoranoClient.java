package org.chorano.client;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.chorano.client.common.ZkConstant;
import org.chorano.client.service.ClientRequestService;
import org.chorano.client.service.ZkService;
import org.chorano.client.service.impl.ClientRequestServiceImpl;
import org.chorano.client.service.impl.ZkServiceImpl;
import org.chorano.client.util.PropUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Entry point of the chorano-client project. Sends http requests to the chorano-servers using
 * a JAX-RS(jersey) client and load balances the requests in a round-robin fashion.
 */
public class ChoranoClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChoranoClient.class);
    private static final String ZK_CLIENT_CONNECTION = PropUtil.getPropVal("zkclient.connection");
    private static final int HTTP_REQUEST_COUNT = PropUtil.getIntPropVal("http.request.count");

    private final ZkService zkService;
    private final ClientRequestService requestService;

    ChoranoClient(ZkService zkService, ClientRequestService requestService) {
        this.zkService = zkService;
        this.requestService = requestService;
    }

    public static Map<String, String> activeServers = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        LOGGER.debug("Connecting to zookeeper client at: {}, timeout: {}", ZK_CLIENT_CONNECTION,
                ZkConstant.ZK_SESSION_TIME_OUT);

        ChoranoClient choranoClient = new ChoranoClient(new ZkServiceImpl(), new ClientRequestServiceImpl());
        ZooKeeper zooKeeper = choranoClient.zkService.connectZookeeper(ZK_CLIENT_CONNECTION,
                ZkConstant.ZK_SESSION_TIME_OUT);
        choranoClient.findAliveServerNodes(zooKeeper);

        if (activeServers.size() == 0) {
            LOGGER.debug("no active chorano server found, Start at least one servers first. Quitting chorano-client");
            System.exit(1);
        }

        choranoClient.requestService.sendHttpRequests(HTTP_REQUEST_COUNT, activeServers);
    }

    /**
     * Gets pairs of active servers in the form (node, data) and sets watchers at the parent node
     * "/chorano" and children nodes(servers) "/chorano/x1..n". The nodes represent servers(x1..xn)
     * and the data represent the connection strings of the servers e.g. (x1, localhost:8111).
     * Whenever a new server is added or a server is shut down at the path the map of (node, data)
     * is updated. Whenever there is a data change (i.e. a server location changes) the map is updated.
     *
     * @param zooKeeper the zookeeper client
     * @return returns a map of all active server nodes and their locations
     */
    public void findAliveServerNodes(ZooKeeper zooKeeper) {
        List<String> children = this.zkService.getChildrenNodes(zooKeeper, "/chorano",
                new ChoranoNodeChangeWatcher(zooKeeper, this));
        LOGGER.debug("Count of active servers: {}", children.size());
        children.forEach(server -> {
            this.zkService.getNodeData(zooKeeper, "/chorano/" + server,
                    new ServerLocationChangeWatcher(server, zooKeeper)).ifPresent(location -> {
                activeServers.put("/chorano/" + server, location);
                LOGGER.debug("server: {} location: {}", server, location);
            });
        });
    }

    static class ChoranoNodeChangeWatcher implements Watcher {
        private final ZooKeeper zooKeeper;
        private final ChoranoClient client;

        ChoranoNodeChangeWatcher(ZooKeeper zooKeeper, ChoranoClient client) {
            this.zooKeeper = zooKeeper;
            this.client = client;
        }

        @Override
        public void process(WatchedEvent event) {
            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                LOGGER.debug("Change detected in one of the servers");
                LOGGER.debug("event: {} ", event);
                this.client.findAliveServerNodes(this.zooKeeper);
            }
        }
    }

    static class ServerLocationChangeWatcher implements Watcher {
        private final String node;
        private final ZooKeeper zooKeeper;

        ServerLocationChangeWatcher(String node, ZooKeeper zooKeeper) {
            this.node = node;
            this.zooKeeper = zooKeeper;
        }

        @Override
        public void process(WatchedEvent event) {
            LOGGER.debug("Change detected in one of the servers");
            LOGGER.debug("event: {} ", event);
            byte[] data = new byte[0];
            if (event.getType() == Event.EventType.NodeDataChanged) {
                try {
                    data = zooKeeper.getData("/chorano/" + this.node, new ServerLocationChangeWatcher(this.node, this.zooKeeper), null);
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
                LOGGER.debug("updating map with location: {}", new String(data));
                activeServers.put("/chorano/" + this.node, new String(data));
            }
            if (event.getType() == Event.EventType.NodeDeleted) {
                LOGGER.debug("Removing server node from zk path: {}", event.getPath());
                activeServers.remove(event.getPath());
            }
        }
    }
}

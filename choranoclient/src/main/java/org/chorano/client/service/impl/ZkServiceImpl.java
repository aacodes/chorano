package org.chorano.client.service.impl;

import org.apache.zookeeper.*;
import org.chorano.client.common.ZkConstant;
import org.chorano.client.service.ZkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * Provides basic zookeeper services such as session establishment.
 */
public class ZkServiceImpl implements ZkService {

    private final Logger logger = LoggerFactory.getLogger(ZkServiceImpl.class);

    /**
     * Connects to zookeeper client with the specified connection string (e.g. localhost:2181)
     * <p>
     * Since session establishment with zookeeper client is asynchronous so we use a CountDownLatch
     * to wait for the connection to establish before the main thread moves on
     * as shown in https://www.baeldung.com/java-zookeeper
     *
     * @param zkClientHost the ip and port number e.g. localhost:8111
     * @param sessionTimeOut session timeout
     * @return returns zookeeper object
     */
    @Override
    public ZooKeeper connectZookeeper(String zkClientHost, int sessionTimeOut) {
        ZooKeeper zooKeeper = null;
        try {
            CountDownLatch latch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(zkClientHost, sessionTimeOut, p -> {
                if (Watcher.Event.KeeperState.SyncConnected == p.getState()) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            System.out.println("Connection not successful. Check if zookeeper is running at the specified ip and port.");
            e.printStackTrace();
        }
        return zooKeeper;
    }

    /**
     * Gets the list of active server nodes from path "/servers"
     *
     * @param zooKeeper the zookeeper client object
     * @return returns a list of active server nodes
     */
    @Override
    public List<String> getListOfActiveServerNodes(ZooKeeper zooKeeper) {
        // get number of children at a defined node.
        List<String> servers = new ArrayList<>();
        try {
            servers = zooKeeper.getChildren("/servers", false);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return servers;
    }

    /**
     * Gets all the children node existing at a specified path. If watcher is non-null then it will be set
     * at the parent node of the specified path.
     *
     * @param zooKeeper zookeeper client object
     * @param path      the path to parent node
     * @param watcher   the watch to be set
     * @return returns a list of children at parent node/path
     */
    public List<String> getChildrenNodes(ZooKeeper zooKeeper, String path, Watcher watcher) {
        List<String> children = new ArrayList<>();
        try {
            children = zooKeeper.getChildren(path, watcher);
        } catch (KeeperException | InterruptedException e) {
            logger.debug("Unable to get children at path: {}\n {}", path, e);
        }
        return children;
    }

    /**
     * Gets (path, data) pair values existing at a specified path. If the watcher is non-null it will be set
     * at all the specified paths.
     *
     * @param zooKeeper the zookeeper client
     * @param path      the list of paths
     * @param watcher   the watch to be set
     * @return returns key-value pairs of (path, data)
     */
    public Optional<String> getNodeData(ZooKeeper zooKeeper, String path, Watcher watcher) {
        byte[] data = null;
        try {
            data = zooKeeper.getData(path, watcher, null);
        } catch (KeeperException | InterruptedException e) {
            logger.debug("Unable to get data at path: {}\n {}", path, e);
        }
        return data == null ? Optional.empty() : Optional.of(new String(data));

    }

    /**
     * TODO: Stop a specific server
     * Creates a stop node at path "/stop". This node signals all the chorano server to shut down.
     *
     * @param zooKeeper zookeeper client
     */
//    @Override
//    public void createStopNode(ZooKeeper zooKeeper) {
//    }

    /**
     * Creates a node at the specified path with the provided data.
     *
     * @param zooKeeper the zookeeper client object
     * @param path the path
     * @param data the data
     * @param creationMode the creation mode. Refer to enum CreateMode
     */
    public void createNode(ZooKeeper zooKeeper, String path, String data, CreateMode creationMode) {
        byte[] bytes = data == null ? new byte[0] : data.getBytes();
        try {
            if (zooKeeper.exists(path, false) == null) {
                zooKeeper.create(path, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

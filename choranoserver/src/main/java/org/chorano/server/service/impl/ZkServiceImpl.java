package org.chorano.server.service.impl;

import org.apache.zookeeper.*;
import org.chorano.server.service.ZkService;
import org.chorano.server.util.ServerStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * Provides basic functionalities of a zookeeper client object.
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
     * @param zkClientHost   the host and port
     * @param sessionTimeout the session timeout
     * @return returns a zookeeper client object
     */
    public ZooKeeper connectZookeeper(String zkClientHost, int sessionTimeout) {
        ZooKeeper zooKeeper = null;
        try {
            CountDownLatch latch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(zkClientHost, sessionTimeout, watchedEvent -> {
                if (Watcher.Event.KeeperState.SyncConnected == watchedEvent.getState()) {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return zooKeeper;
    }

    /**
     * Creates a node if not exists and sets a watcher
     *
     * @param zooKeeper the zookeeper client object
     * @param path the path
     * @param watcher the watcher
     */
    public void setWatcher(ZooKeeper zooKeeper, String path, Watcher watcher) {
        try {
            if (zooKeeper.exists(path, null) == null) {
                zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            int version = zooKeeper.exists(path, watcher).getVersion();
            logger.debug("server watcher installing.. {}", version);

        } catch (KeeperException | InterruptedException e) {
            logger.debug("Unable to set watcher at path: {}\n{}", path, e);
        }
    }


    /**
     * When the server starts it registers itself in zookeeper by creating a node
     * at path "/chorano/n_1..x" with ip(localhost) and port. The node created is persistent.
     *
     * @param zooKeeper the zookeeper client instance
     * @param path      the node path
     * @param data      the port at which the server itself is running
     */
    public void registerInZookeeper(ZooKeeper zooKeeper, String path, String data) {
        try {
            if (zooKeeper.exists(path, false) == null) {
                zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            zooKeeper.create(path + "/x_", data.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

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

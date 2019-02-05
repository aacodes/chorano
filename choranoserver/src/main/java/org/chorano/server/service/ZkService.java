package org.chorano.server.service;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

/**
 * Provides functionalities to interact with zookeeper client
 */
public interface ZkService {

    ZooKeeper connectZookeeper(String zkClientConnection, int sessionTimeout);

    void registerInZookeeper(ZooKeeper zooKeeper, String path, String data);

    void createNode(ZooKeeper zooKeeper, String path, String data, CreateMode creationMode);

    void setWatcher(ZooKeeper zooKeeper, String path, Watcher watcher);

}

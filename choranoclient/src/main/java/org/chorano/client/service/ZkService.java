package org.chorano.client.service;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ZkService {

    ZooKeeper connectZookeeper(String zkClientHost, int sessionTimeOut);

    List<String> getListOfActiveServerNodes(ZooKeeper zooKeeper);

    List<String> getChildrenNodes(ZooKeeper zooKeeper, String path, Watcher watcher);

    Optional<String> getNodeData(ZooKeeper zooKeeper, String path, Watcher watcher);

    void createNode(ZooKeeper zooKeeper, String path, String data, CreateMode creationMode);

}

package org.chorano.server;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.chorano.server.common.ZkConstant;
import org.chorano.server.service.ServerService;
import org.chorano.server.service.impl.SocketServerServiceImpl;
import org.chorano.server.service.impl.ZkServiceImpl;
import org.chorano.server.util.PropUtil;
import org.chorano.server.util.ServerStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ChoranoServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChoranoServer.class);

    public static void main(String[] args) {


        LOGGER.debug("Server >> Specify the port to listen to: ");
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();

        String zkClientConnection = PropUtil.getPropVal("zkclient.connection");
        LOGGER.debug("Connecting to zookeeper client at: {}, session timeout: {}", zkClientConnection,
                ZkConstant.ZK_SESSION_TIME_OUT);
        ZkServiceImpl zkServiceImpl = new ZkServiceImpl();
        ZooKeeper zooKeeper = zkServiceImpl.connectZookeeper(zkClientConnection, ZkConstant.ZK_SESSION_TIME_OUT);

        LOGGER.debug("Creating root persistant node named \"/chorano\" in zk server");
        LOGGER.debug("Registering server at the root node e.g. \"/chorano/x_1\"");
        zkServiceImpl.registerInZookeeper(zooKeeper, "/chorano",
                "localhost:" + String.valueOf(port));

        LOGGER.debug("Starting the chorano server so that it keeps listening to port: " + port);
        ServerService serverService = new SocketServerServiceImpl();
        serverService.startChoranoServer(port);

    }

}

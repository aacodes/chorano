package org.chorano.server.service.impl;

import org.chorano.server.request.handler.HttpRequestHandler;
import org.chorano.server.util.PropUtil;
import org.chorano.server.service.ServerService;
import org.chorano.server.util.ServerStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SocketServerServiceImpl implements ServerService {

    private static final int SERVER_THREAD_POOL_SIZE =
            PropUtil.getIntPropVal("request.handler.thread.pool.size");
    private final Logger logger = LoggerFactory.getLogger(SocketServerServiceImpl.class);

    @Override
    public void startChoranoServer(int port) {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            // TODO: implement our own ExecutorService class
            ExecutorService service = Executors.newFixedThreadPool(SERVER_THREAD_POOL_SIZE);
            while (!ServerStatusUtil.instance().isStopServer()) {
                Socket socket = serverSocket.accept();
                service.execute(new HttpRequestHandler(socket));
                this.logger.debug("Main thread exiting: " + Thread.currentThread().getName());
            }
            this.logger.debug("Shutting down server");
            service.shutdown();
            try {
                if (!service.awaitTermination(5000, TimeUnit.SECONDS)) {
                    this.logger.debug("Forced thread shutdown");
                    service.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.logger.debug("Unable to shut down threads properly: {}", e);
            }

        } catch (IOException e) {
            this.logger.debug("An I/O error occurs when opening the socket: {}", e);
        }
    }
}

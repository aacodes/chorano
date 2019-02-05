package org.chorano.client.service.impl;

import org.chorano.client.service.ClientRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientRequestServiceImpl implements ClientRequestService {

    private final Logger logger = LoggerFactory.getLogger(ClientRequestServiceImpl.class);

    /**
     * Sends http requests using a jax-rs(jersey) client to the alive servers
     *
     * @param numOfRequests the number of http requests to be sent
     * @param aliveServers map consisting of servers and their locations
     */
    @Override
    public void sendHttpRequests(int numOfRequests, Map<String, String> aliveServers) {
        long start = System.currentTimeMillis();
        Client client = ClientBuilder.newClient();
        AtomicInteger num = new AtomicInteger();
        while (num.get() < numOfRequests) {
            aliveServers.forEach((node, host) -> {
                String url = "http://" + host;
                this.logger.debug("Sending http request no. {} to chorano server: {} at url {}", num, node, url);
                try {
                    String requestEntity = "Client request no: " + num;
                    Response response = client.target(url)
                            .request()
                            .post(Entity.entity(requestEntity, MediaType.TEXT_PLAIN));
                    this.logger.debug("Node: {} Response entity: {}", node, response.readEntity(String.class));
                } catch (Exception e) {
                    this.logger.debug("Failed to send request at server: {} url: {}", node, url);
                }
                num.getAndIncrement();
            });
            if (aliveServers.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                this.logger.debug("Interrupted while sleeping between requests {}", e);
            }
        }

        long end = System.currentTimeMillis();
        this.logger.debug("Time elapsed: {} minutes", (end - start)/(60 * 1000));
        client.close();
    }

    /**
     * Signaling all servers to shutdown by writing "stop" in their output stream
     *
     * @param serverConnection server location
     */
    public void sendStopSignal(String serverConnection) {
        String[] address = serverConnection.split(":");
        this.logger.debug("Shutting down at {} port {}", address[0], address[1]);
        try (Socket socket = new Socket(address[0], Integer.valueOf(address[1]))) {
            try (BufferedOutputStream os = new BufferedOutputStream(socket.getOutputStream())) {
                os.write("stop".getBytes());
                os.flush();
                socket.close();
            }
        } catch (IOException e) {
            this.logger.debug("I/O expcetion while writing to outputstream at {}", serverConnection);
        }
    }
}

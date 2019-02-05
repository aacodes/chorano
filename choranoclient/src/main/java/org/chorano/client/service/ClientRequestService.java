package org.chorano.client.service;

import java.util.Map;

public interface ClientRequestService {

    /**
     * Sends http requests using a jax-rs(jersey) client to the alive servers
     *
     * @param numOfRequests the number of http requests to be sent
     * @param servers map consisting of servers and their locations
     */
    void sendHttpRequests(int numOfRequests, Map<String, String> servers);

    /**
     * Signaling all servers to shutdown by writing "stop" in their output stream
     *
     * @param serverConnection server location
     */
    void sendStopSignal(String serverConnection);

}

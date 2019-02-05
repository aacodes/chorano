package org.chorano.server.request.handler;

import java.net.Socket;

/**
 * Extend the abstract request to handle different protocols (e.g. http, tcp)
 */
public abstract class AbstractRequestHandler {

    public abstract void handle(Socket socket);

}

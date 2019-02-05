package org.chorano.server.util;

/**
 * Uses initialization on demand holder pattern to initialize a single server status object.
 * All property values are retrieved from this object.
 *
 * Keeps a record of the status of the server.
 */
public final class ServerStatusUtil {

    private ServerStatusUtil() {

    }

    private boolean stopServer;


    public boolean isStopServer() {
        return this.stopServer;
    }

    public void stopServer(boolean stopServer) {
        this.stopServer = stopServer;
    }

    private static final class Holder {
        static final ServerStatusUtil INSTANCE = new ServerStatusUtil();
    }

    public static ServerStatusUtil instance() {
        return Holder.INSTANCE;
    }

}

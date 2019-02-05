package org.chorano.server.request.handler;

import org.chorano.server.util.ServerStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Handles http request and writes a http response back.
 */
public class HttpRequestHandler extends AbstractRequestHandler implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
    private static final int CONTENT_LENGTH_BEGIN_INDEX = 16;
    private final Socket socket;

    public HttpRequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        handle(this.socket);
    }

    @Override
    public void handle(Socket socket) {
        this.logger.debug("Handler thread: {}", Thread.currentThread().getName());
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()))) {
            String line;
            int contentLength = 0;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null && line.length() > 0) {
                if (contentLength == 0 && line.contains("Content-Length")) {
                    contentLength = Integer.parseInt(line.substring(CONTENT_LENGTH_BEGIN_INDEX));
                }
                sb.append(line);
            }
            if (!ServerStatusUtil.instance().isStopServer()) {
                char[] content = new char[contentLength];
                reader.read(content);
                String entity = new String(content);
                this.logger.debug("{}\nEntity payload: {}", sb.toString(), entity);
                if (entity.equals("stop")) {
                    this.logger.debug("recieved signal to stop server");
                    ServerStatusUtil.instance().stopServer(true);
                }
                OutputStreamWriter writer = new OutputStreamWriter(this.socket.getOutputStream());
                // jax-rs client expects a well formed http response
                writer.write("HTTP/1.1 200 \r\n");
                writer.write("Content-Type: text/plain\r\n");
                writer.write("Connection: close\r\n");
                writer.write("\r\n");
                writer.write("Serving: " + entity);
                writer.flush();
            }
            this.socket.close();
            this.logger.debug("Socket connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

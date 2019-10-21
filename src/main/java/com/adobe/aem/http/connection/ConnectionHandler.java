package com.adobe.aem.http.connection;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import com.adobe.aem.http.request.RequestHandler;
import com.google.inject.assistedinject.Assisted;
import org.apache.http.*;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.inject.Inject;

/**
 * Handles a connection which can be used for multiple HTTP requests when KeepAlive
 */
public class ConnectionHandler implements Runnable {

    private static final Logger log = LogManager.getLogger(ConnectionHandler.class);

    private static final int KEEP_ALIVE_TIMEOUT = 15000;

    private final Socket socket;
    private RequestHandler requestHandler;

    private boolean keepAlive;

    @Inject
    public ConnectionHandler(@Assisted Socket socket, RequestHandler handler) {
        this.socket = socket;
        this.requestHandler = handler;
        this.keepAlive = true;
    }

    public void run() {
        try {
            this.socket.setSoTimeout(KEEP_ALIVE_TIMEOUT);
        } catch (SocketException e) {
            //this should not happen
            throw new RuntimeException(e);
        }
        while (keepAlive) {
            try {
                long processTime = System.currentTimeMillis();

                SessionInputBufferImpl buffer = new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 1024);
                buffer.bind(socket.getInputStream());
                DefaultHttpRequestParser httpRequestParser = new DefaultHttpRequestParser(buffer);
                HttpRequest request = httpRequestParser.parse();

                Header connectionHeader = request.getFirstHeader("Connection");
                if (connectionHeader == null || !"keep-alive".equalsIgnoreCase(connectionHeader.getValue())) {
                    keepAlive = false;
                }
                requestHandler.handle(request, socket);
                processTime = System.currentTimeMillis() - processTime;//will include keep-alive time for subsequent requests
                log.info("Request processed in {} ms", processTime);
            } catch (ConnectionClosedException | SocketException cce) {
                log.info("Client closed connection");
                keepAlive = false;
            } catch (SocketTimeoutException stex) {
                log.info("Keep-Alive timeout, closing connection");
                keepAlive = false;
            } catch (IOException | HttpException e) {
                log.error("Unexpected exception", e);
                keepAlive = false;
            } finally {
                if (!keepAlive) {
                    try {
                        log.debug("Closing connection");
                        socket.close();
                    } catch (IOException e) {
                        //not much to recover here
                    }
                } else {
                    log.debug("Keeping connection alive");
                }
            }
        }
    }
}

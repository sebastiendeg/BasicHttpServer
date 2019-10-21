package com.adobe.aem.http;

import com.adobe.aem.http.connection.ConnectionHandlerFactory;
import com.adobe.aem.http.request.ServerBusyRequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;

/**
 * Server accepting connections and dispatching them immediately to an instance of
 *
 * @{@link com.adobe.aem.http.connection.ConnectionHandler} in a separate thread for processing.
 */
public class HttpServer {

    private static Logger log = LogManager.getLogger(HttpServer.class);

    private volatile boolean runServer = true;// method stop can be called from a separate thread.

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private ServerConfig serverConfig;
    private ConnectionHandlerFactory chFactory;

    @Inject
    public HttpServer(ServerConfig serverConfig, ConnectionHandlerFactory connectionHandlerFactory) {
        this.serverConfig = serverConfig;
        this.chFactory = connectionHandlerFactory;
    }

    //blocking method
    public void run() {

        initExecutor();
        try {
            serverSocket = new ServerSocket(serverConfig.getPort());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Server Socket", e);
        }
        log.info("Ready to accept connections at http://{}:{}", InetAddress.getLoopbackAddress().getHostAddress(), serverConfig.getPort());
        while (runServer) {
            try {
                Socket socket = serverSocket.accept();
                log.debug("Incoming connection received from {}", socket.getInetAddress().getHostAddress());
                try {
                    pool.execute(chFactory.create(socket));
                } catch (RejectedExecutionException rex) {
                    log.error("connection rejected from pool, return 503");
                    //don't create a new thread to prevent DDOS. We could simply refuse connection but this is more elegant
                    //and provide more info to the client.
                    new ServerBusyRequestHandler().handle(socket);
                }
            } catch (InterruptedIOException ex) {
                if (!runServer) {
                    log.info("InterruptedIOException, Shutting down server");
                } else {
                    log.fatal("InterruptedIOException (Not expected), shutting down server", ex);
                    stop();
                }
            } catch (SocketException e) {
                //this will happen if this.stop is called and we close the ServerSocket.
                if (!runServer) {
                    log.info("SocketException, shutting down server");
                } else {
                    log.error("SocketException (Not expected), shutting down server", e);
                    stop();
                }
            } catch (Throwable e) {
                log.error("Unexpected non-fatal error, attempting to continue", e);
            }
        }
    }

    public void stop() {
        runServer = false;
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                //not much to do here other than logging
                log.error("Error whilst closing server socket", e);
            }
        }
        pool.shutdownNow();
    }

    /**
     * Unhandled exceptions thrown by tasks will be swallowed and disappear. We assume that we are expecting this behaviour.
     */
    private void initExecutor() {
        if (pool == null) {
            //SynchronousQueue for direct HandOff
            SynchronousQueue<Runnable> workQueue = new SynchronousQueue<>();
            //keep a smaller number of threads ready for execution and create up to x threads based on config.
            //if we reach the maximum number of threads, tasks will be rejected which is what we want in this scenario.
            pool = new ThreadPoolExecutor(10, serverConfig.getThreadCount(), 1L, TimeUnit.SECONDS, workQueue);
        }
    }


}

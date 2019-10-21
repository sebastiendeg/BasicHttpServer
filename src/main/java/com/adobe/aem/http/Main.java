package com.adobe.aem.http;

import com.google.inject.Guice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        logger.info("Web Server Starting...");

        //create a single server accepting connections. We could create more than one if needed (Http, Https, Control Port, ...)
        HttpServer httpServer = Guice.createInjector(new WiringModule()).getInstance(HttpServer.class);

        //shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop));

        httpServer.run();
        logger.info("Web Server stopped, bye!");
    }
}

package com.adobe.aem.http.connection;

import java.net.Socket;

public interface ConnectionHandlerFactory {

    ConnectionHandler create(Socket socket);

}

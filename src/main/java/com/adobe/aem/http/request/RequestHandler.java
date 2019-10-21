package com.adobe.aem.http.request;

import com.google.inject.ImplementedBy;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;

import java.io.IOException;
import java.net.Socket;

@ImplementedBy(FileSystemRequestHandler.class)
public interface RequestHandler {

    void handle(HttpRequest request, Socket socket) throws IOException, HttpException;
}

package com.adobe.aem.http.request;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;

import java.io.IOException;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ServerBusyRequestHandler {

    public void handle(Socket socket) throws IOException, HttpException {
        HttpResponse response = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 503, "Service Unavailable"), new BasicHttpContext());
        response.setHeader(new BasicHeader("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())));
        response.setHeader(new BasicHeader("Connection", "close"));
        response.setHeader(new BasicHeader("Content-Length", "0"));

        SessionOutputBufferImpl buffer = new SessionOutputBufferImpl(new HttpTransportMetricsImpl(), 1024);
        DefaultHttpResponseWriter responseWriter = new DefaultHttpResponseWriter(buffer);
        buffer.bind(socket.getOutputStream());
        responseWriter.write(response);
        buffer.flush();
        socket.getOutputStream().close();
    }
}

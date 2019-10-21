package com.adobe.aem.http.request;

import com.adobe.aem.http.ServerConfig;
import com.adobe.aem.http.connection.ConnectionHandler;
import com.google.inject.Inject;
import org.apache.http.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.ContentLengthOutputStream;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles a single HTTP Request GET and fetch content from file system
 */
public class FileSystemRequestHandler implements RequestHandler {

    private static final Logger log = LogManager.getLogger(ConnectionHandler.class);

    private ServerConfig serverConfig;

    @Inject
    public FileSystemRequestHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void handle(HttpRequest request, Socket socket) throws IOException, HttpException {

        RequestLine requestLine = request.getRequestLine();
        log.debug("handle request {} {}", requestLine.getMethod(), requestLine.getUri());

        //build the response, if something goes wrong we can then return 500
        HttpResponse response = buildDefaultResponse(request);
        log.info("response {}", response.getStatusLine());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();//just so that we can output for trace
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        SessionOutputBufferImpl buffer = new SessionOutputBufferImpl(metrics, 8192);
        DefaultHttpResponseWriter responseWriter = new DefaultHttpResponseWriter(buffer);
        buffer.bind(baos);
        responseWriter.write(response);

        if (null != response.getEntity()) {
            StrictContentLengthStrategy contentLengthStrategy = new StrictContentLengthStrategy();
            long len = contentLengthStrategy.determineLength(response);
            OutputStream outputStream = new ContentLengthOutputStream(buffer, len);
            response.getEntity().writeTo(outputStream);
            outputStream.close();
        }
        if (log.isTraceEnabled()) {
            log.trace("response {}", new String(baos.toByteArray()));
        }

        baos.writeTo(socket.getOutputStream());
        socket.getOutputStream().flush();
    }

    private HttpResponse buildDefaultResponse(HttpRequest request) {
        HttpResponse response = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"), new BasicHttpContext());
        resolveEntity(response, request);
        response.setHeader(new BasicHeader("Content-Encoding", "identity"));//not mandatory
        response.setHeader(new BasicHeader("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now())));
        return response;
    }

    private void resolveEntity(HttpResponse response, HttpRequest request) {
        String uri = request.getRequestLine().getUri();
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("error decoding the URI {}", uri);
        }
        String rootFolder = serverConfig.getRootFolder();
        File file = new File(rootFolder + uri);
        HttpEntity entity;

        if (!file.exists()) {
            entity = build404();
            response.setReasonPhrase("Not Found");
            response.setStatusCode(404);
        } else if (file.isDirectory()) {
            entity = buildFolderList(file);
        } else {
            entity = new FileEntity(file);//uses Stream so not loading all file in memory
            response.setHeader(new BasicHeader("Content-Type", getContentType(file.getPath())));
        }
        response.setEntity(entity);
        response.setHeader(new BasicHeader("Content-Length", Long.toString(entity.getContentLength())));
    }


    private String getContentType(String filePath) {
        return URLConnection.guessContentTypeFromName(filePath);//rely on file name/extension.
        //Below lines require to open the file and read the magic-bytes.
        //to not consume too many IO (especially when files are (ex: AWS-S3)), we would need to implement a lazy-load InputStream and cache what we have already read for later.
        //InputStream is = new BufferedInputStream(new FileInputStream(filePath));
        //return URLConnection.guessContentTypeFromStream(is);
    }


    //build a page that list the files and directories.
    private HttpEntity buildFolderList(File file) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><head><title>").append(file.getName()).append("</title></head>");
        builder.append("<body>");
        if (file.getParent() != null) {
            builder.append("<a href=\"/").append(file.getParentFile().getPath()).append("\">..</a>");
            builder.append("</br>");
        }
        for (File f : file.listFiles()) {
            builder.append("<a href=\"./").append(file.getName()).append("/").append(f.getName()).append("\">").append(f.getName()).append("</a>");
            builder.append("</br>");
        }
        builder.append("</body></html>");
        StringEntity entity = new StringEntity(builder.toString(), "UTF-8");
        entity.setContentType("text/html");
        return entity;
    }

    //build a page that list the files and directories.
    private HttpEntity build404() {
        String html = "<html><head><title>404 Not Found</title></head><body>NOT FOUND</body></html>";
        StringEntity entity = new StringEntity(html, "UTF-8");
        entity.setContentType("text/html");
        return entity;
    }
}

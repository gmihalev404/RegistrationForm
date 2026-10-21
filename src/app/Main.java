package app;

import app.handler.IndexHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {

        InetSocketAddress address = new InetSocketAddress(8080);

        HttpServer server = HttpServer.create(address, 10);

        server.createContext("/", new IndexHandler());

        server.start();
    }
}

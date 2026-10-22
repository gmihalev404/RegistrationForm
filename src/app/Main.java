package app;

import app.handler.IndexHandler;
import app.handler.LoginHandler;
import app.handler.RegistrationHandler;
import app.util.DatabaseInitializer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws IOException, SQLException {
        InetSocketAddress address = new InetSocketAddress(8080);
        HttpServer server = HttpServer.create(address, 10);

        server.createContext("/", new IndexHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/register", new RegistrationHandler());
//        server.createContext("/profile", new ProfileHandler());
//        server.createContext("/logout", new LogoutHandler());

        DatabaseInitializer.initializeDatabase();

        server.start();
    }
}
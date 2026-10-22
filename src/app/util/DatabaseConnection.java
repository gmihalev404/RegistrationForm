package app.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = System.getenv("DB_URL") != null
            ? System.getenv("DB_URL")
            : "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";

    private static final String DB_NAME = "registration_db";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";

    static {
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        String dbUrl = URL.substring(0, URL.indexOf('?'))
                + DB_NAME
                + URL.substring(URL.indexOf('?'));

        return DriverManager.getConnection(dbUrl, USER, PASSWORD);
    }

    public static Connection getServerConnection() throws SQLException {

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
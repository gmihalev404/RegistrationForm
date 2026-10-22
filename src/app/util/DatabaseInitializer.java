package app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initializeDatabase() {
        String filePath = "src/sql/schema.sql";

        try {
            String sqlScript = Files.readString(Path.of(filePath));
            String[] queries = sqlScript.split(";");

            try (Connection conn = DatabaseConnection.getServerConnection();
                 Statement stmt = conn.createStatement()) {

                for (String query : queries) {
                    if (!query.trim().isEmpty()) {
                        stmt.execute(query.trim());
                    }
                }
                System.out.println("Database initialized!");

            }
        } catch (IOException e) {
            System.err.println("Could not read schema.sql file: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Initialization error: " + e.getMessage());
        }
    }
}
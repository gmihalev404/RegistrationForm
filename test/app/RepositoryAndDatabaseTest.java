package app;

import app.model.User;
import app.repository.UserRepository;
import app.util.DatabaseConnection;
import app.util.DatabaseInitializer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepositoryAndDatabaseTest {

    @Test
    void repositoryMapsUserAndHandlesMissingRecords() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("id")).thenReturn(42L);
        when(rs.getString("first_name")).thenReturn("Ada");
        when(rs.getString("last_name")).thenReturn("Lovelace");
        when(rs.getString("email")).thenReturn("ada@example.com");
        when(rs.getString("password")).thenReturn("secret");

        try (MockedStatic<DatabaseConnection> db = mockStatic(DatabaseConnection.class)) {
            db.when(DatabaseConnection::getConnection).thenReturn(connection);
            UserRepository repo = new UserRepository();

            User foundById = repo.findById(42L).orElseThrow();
            assertEquals(42L, foundById.getId());
            assertEquals("Ada", foundById.getFirstName());
            assertEquals("Lovelace", foundById.getLastName());
            assertEquals("ada@example.com", foundById.getEmail());
            assertEquals("secret", foundById.getPassword());
            verify(stmt).setLong(1, 42L);

            assertTrue(repo.findByEmail("ada@example.com").isPresent());
            verify(stmt).setString(1, "ada@example.com");

            assertTrue(repo.findByEmailAndPassword("ada@example.com", "secret").isPresent());
            verify(stmt).setString(2, "secret");

            when(rs.next()).thenReturn(false);
            assertTrue(repo.findById(200L).isEmpty());
            assertTrue(repo.findByEmail("unknown@example.com").isEmpty());
            assertTrue(repo.findByEmailAndPassword("unknown@example.com", "wrong").isEmpty());
        }
    }

    @Test
    void repositoryInsertUpdateNameAndPasswordUsePreparedStatements() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        try (MockedStatic<DatabaseConnection> db = mockStatic(DatabaseConnection.class)) {
            db.when(DatabaseConnection::getConnection).thenReturn(connection);
            UserRepository repo = new UserRepository();

            repo.save(new User("Ada", "Lovelace", "ada@example.com", "secret"));
            verify(stmt).setString(1, "Ada");
            verify(stmt).setString(2, "Lovelace");
            verify(stmt).setString(3, "ada@example.com");
            verify(stmt).setString(4, "secret");

            repo.save(new User(42L, "New", "Name", "ada@example.com", "newpass"));
            verify(stmt).setLong(5, 42L);
            verify(stmt).setString(1, "New");

            repo.updateName(42, "First", "Last");
            verify(stmt).setInt(3, 42);
            verify(stmt).setString(1, "First");
            verify(stmt).setString(2, "Last");

            repo.updatePassword(42, "changed");
            verify(stmt).setInt(3, 42);
            verify(stmt).setInt(2, 42);
            verify(stmt).setString(1, "changed");
            verify(stmt, times(4)).executeUpdate();
        }
    }

    @Test
    void initializerExecutesSchemaStatementsWithoutRealDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        when(connection.createStatement()).thenReturn(stmt);

        try (MockedStatic<DatabaseConnection> db = mockStatic(DatabaseConnection.class)) {
            db.when(DatabaseConnection::getServerConnection).thenReturn(connection);
            DatabaseInitializer.initializeDatabase();
        }
        verify(stmt, times(4)).execute(anyString());
        verify(connection).close();
        verify(stmt).close();
    }
}

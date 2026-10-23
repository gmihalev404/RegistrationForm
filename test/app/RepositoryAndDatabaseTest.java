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
        when(rs.getString("first_name")).thenReturn("T");
        when(rs.getString("last_name")).thenReturn("T");
        when(rs.getString("email")).thenReturn("test@t.com");
        when(rs.getString("password")).thenReturn("secret");

        try (MockedStatic<DatabaseConnection> db = mockStatic(DatabaseConnection.class)) {
            db.when(DatabaseConnection::getConnection).thenReturn(connection);
            UserRepository repo = new UserRepository();

            User foundById = repo.findById(42L).orElseThrow();
            assertEquals(42L, foundById.getId());
            assertEquals("T", foundById.getFirstName());
            assertEquals("T", foundById.getLastName());
            assertEquals("test@t.com", foundById.getEmail());
            assertEquals("secret", foundById.getPassword());
            verify(stmt).setLong(1, 42L);

            assertTrue(repo.findByEmail("test@t.com").isPresent());
            verify(stmt).setString(1, "test@t.com");

            assertTrue(repo.findByEmailAndPassword("test@t.com", "secret").isPresent());
            verify(stmt).setString(2, "secret");

            when(rs.next()).thenReturn(false);
            assertTrue(repo.findById(200L).isEmpty());
            assertTrue(repo.findByEmail("test.com").isEmpty());
            assertTrue(repo.findByEmailAndPassword("test.com", "wrong").isEmpty());
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

            repo.save(new User("T", "T", "test@t.com", "secret"));
            verify(stmt).setString(1, "T");
            verify(stmt).setString(2, "T");
            verify(stmt).setString(3, "test@t.com");
            verify(stmt).setString(4, "secret");

            repo.save(new User(42L, "T", "T", "test@t.com", "newpass"));
            verify(stmt).setLong(5, 42L);
            verify(stmt, times(2)).setString(1, "T");

            repo.updateName(42, "T", "T");
            verify(stmt).setInt(3, 42);
            verify(stmt, times(3)).setString(1, "T");
            verify(stmt, times(3)).setString(2, "T");

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

package app;

import app.model.User;
import app.repository.UserRepository;
import app.util.DatabaseConnection;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepositoryAndDatabaseTest {

    @Test
    void repositoryFindsUsersAndHandlesMissingRecords() throws Exception {
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
        when(rs.getString("password")).thenReturn("secret1");

        try (MockedStatic<DatabaseConnection> db = mockStatic(DatabaseConnection.class)) {
            db.when(DatabaseConnection::getConnection).thenReturn(connection);
            UserRepository repo = new UserRepository();

            User found = repo.findById(42L).orElseThrow();
            assertEquals(42L, found.getId());
            assertEquals("T", found.getFirstName());
            assertEquals("test@t.com", found.getEmail());
            assertTrue(repo.findByEmail("test@t.com").isPresent());
            assertTrue(repo.findByEmailAndPassword("test@t.com", "secret1").isPresent());
            verify(stmt).setLong(1, 42L);
            verify(stmt).setString(2, "secret1");

            when(rs.next()).thenReturn(false);
            assertTrue(repo.findById(99L).isEmpty());
            assertTrue(repo.findByEmail("test.com").isEmpty());
            assertTrue(repo.findByEmailAndPassword("test", "wrong").isEmpty());
        }
    }

    @Test
    void repositoryInsertsAndUpdatesUser() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(stmt);

        try (MockedStatic<DatabaseConnection> db = mockStatic(DatabaseConnection.class)) {
            db.when(DatabaseConnection::getConnection).thenReturn(connection);
            UserRepository repo = new UserRepository();

            repo.save(new User("T", "T", "test@t.com", "secret1"));
            verify(stmt).setString(1, "T");
            verify(stmt).setString(2, "T");
            verify(stmt).setString(3, "test@t.com");
            verify(stmt).setString(4, "secret1");

            repo.save(new User(42L, "T", "T", "test@t.com", "newpass"));
            verify(stmt).setLong(5, 42L);
            verify(stmt).setString(4, "newpass");
            verify(stmt, times(2)).executeUpdate();
        }
    }
}

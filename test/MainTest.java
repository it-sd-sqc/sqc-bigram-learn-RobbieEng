import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

import edu.cvtc.bigram.*;

@SuppressWarnings({"SpellCheckingInspection"})
class MainTest {
  @Test
  void createConnection() {
    assertDoesNotThrow(
        () -> {
          Connection db = Main.createConnection();
          assertNotNull(db);
          assertFalse(db.isClosed());
          db.close();
          assertTrue(db.isClosed());
        }, "Failed to create and close connection."
    );
  }

  @Test
  void reset() {
    Main.reset();
    assertFalse(Files.exists(Path.of(Main.DATABASE_PATH)));
  }

  @Test
  void mainArgs() {
    assertAll(
        () -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setOut(new PrintStream(out));
          Main.main(new String[]{"--version"});
          String output = out.toString();
          assertTrue(output.startsWith("Version "));
        },
        () -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setOut(new PrintStream(out));
          Main.main(new String[]{"--help"});
          String output = out.toString();
          assertTrue(output.startsWith("Add bigrams"));
        },
        () -> assertDoesNotThrow(() -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setErr(new PrintStream(out));
          Main.main(new String[]{"--reset"});
          String output = out.toString();
          assertTrue(output.startsWith("Expected"));
        }),
        () -> assertDoesNotThrow(() -> Main.main(new String[]{"./sample-texts/non-existant-file.txt"})),
        () -> assertDoesNotThrow(() -> Main.main(new String[]{"./sample-texts/empty.txt"}))
    );
  }

  // TODO: Create your test(s) below. /////////////////////////////////////////

  public class BigramTest {
    private Connection db;


    @BeforeEach
    public void setup() throws SQLException {
        db = DriverManager.getConnection("jdbc:sqlite:test.db");
        db.createStatement().execute("CREATE TABLE IF NOT EXISTS words (id INTEGER PRIMARY KEY, string TEXT NOT NULL)");
        db.createStatement().execute("CREATE TABLE IF NOT EXISTS bigrams (id INTEGER PRIMARY KEY, words_id INTEGER, next_words_id INTEGER)");
    }

    @Test
    public void testAddDuplicateBigram() throws SQLException {
        int wordId1 = Main.getId(db, "hello");
        int wordId2 = Main.getId(db, "world");
        
        Main.addBigram(db, wordId1, wordId2); // First insert
        int initialCount = getBigramCount(db);

        Main.addBigram(db, wordId1, wordId2); // Attempt to insert duplicate
        int finalCount = getBigramCount(db);

        assertEquals(initialCount, finalCount, "Duplicate bigram should not increase count");
    }

    private int getBigramCount(Connection db) throws SQLException {
        var resultSet = db.createStatement().executeQuery("SELECT COUNT(*) AS count FROM bigrams");
        return resultSet.getInt("count");
    }
  }
}
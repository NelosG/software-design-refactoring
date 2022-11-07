package ru.akirakozov.sd.refactoring.dao;

import org.junit.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

import static org.junit.Assert.*;

public class DatabaseTest {
    private Path tempDir;
    private String testDbUrl;

    @Before
    public void initDbTempDir() {
        try {
            tempDir = Files.createTempDirectory(UUID.randomUUID().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        testDbUrl = "jdbc:sqlite:" + tempDir.resolve("tmp.db");
    }

    @After
    public void removeDbTempDir() {
        try {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testConnection() {
        Database database = new Database(testDbUrl);
        int result = database.query("SELECT 1", resultSet -> resultSet.getInt(1));
        assertEquals(1, result);
    }

    private void initTestTable(Database database) {
        database.update("DROP TABLE IF EXISTS Products");
        database.update("CREATE TABLE Products" +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                " name           TEXT    NOT NULL, " +
                " price          INT     NOT NULL)");
    }

    @Test 
    public void testDdl() {
        Database database = new Database(testDbUrl);
        initTestTable(database);
    }

    @Test
    public void testInsertAndSelect() {
        Database database = new Database(testDbUrl);
        initTestTable(database);
        database.update("INSERT INTO Products(name, price) VALUES ('test', 5)");
        int result = database.query("SELECT COUNT(*) FROM Products",
                resultSet -> resultSet.getInt(1));
        assertEquals(1, result);
    }

    @Test
    public void testUpdateAndSelect() {
        Database database = new Database(testDbUrl);
        initTestTable(database);
        database.update("INSERT INTO Products(name, price) VALUES ('test', 5)");
        database.update("UPDATE Products SET price=555");
        int result = database.query("SELECT MAX(price) FROM Products",
                resultSet -> resultSet.getInt(1));
        assertEquals(555, result);
    }
}

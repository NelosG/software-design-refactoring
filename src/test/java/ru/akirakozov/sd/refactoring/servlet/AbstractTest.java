package ru.akirakozov.sd.refactoring.servlet;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.Mock;
import ru.akirakozov.sd.refactoring.dao.Database;
import ru.akirakozov.sd.refactoring.entity.Product;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public abstract class AbstractTest {
    protected static Database database;
    protected static Path directory;

    @Mock
    protected HttpServletRequest request;

    @Mock
    protected HttpServletResponse response;

    @BeforeClass
    public static void initDb() {
        try {
            directory = Files.createTempDirectory(UUID.randomUUID().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        database = new Database("jdbc:sqlite:" + directory.resolve("tmp.db"));
    }

    @AfterClass
    public static void removeDb() {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected List<Product> executeSelectAll() {
        return database.query("SELECT * FROM PRODUCT", resultSet -> {
            List<Product> result = new ArrayList<>();
            while (resultSet.next()) {
                String  name = resultSet.getString("name");
                int price  = resultSet.getInt("price");
                result.add(new Product(name, price));
            }
            return result;
        });
    }

    @Before
    public void initTestDb() {
        database.update("DROP TABLE IF EXISTS PRODUCT");
        database.update("CREATE TABLE PRODUCT" +
                "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                " NAME           TEXT    NOT NULL, " +
                " PRICE          INT     NOT NULL)");
    }

    void addProduct(Product product) {
        database.update(String.format("INSERT INTO PRODUCT(NAME, PRICE) VALUES ('%s', %d)", product.getName(), product.getPrice()));
    }


    protected String callServletWithValidationAndGetResponce() {
        boolean[] contentTypeSet = new boolean[]{false};
        boolean[] statusSet = new boolean[]{false};

        doAnswer(answer -> {
            String contentType = answer.getArgument(0);
            assertEquals("text/html", contentType);
            contentTypeSet[0] = true;
            return null;
        }).when(response).setContentType(anyString());

        doAnswer(answer -> {
            int statusCode = answer.getArgument(0);
            assertEquals(HttpServletResponse.SC_OK, statusCode);
            statusSet[0] = true;
            return null;
        }).when(response).setStatus(anyInt());

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        try {
            when(response.getWriter()).thenReturn(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        runSupport();

        assertTrue(contentTypeSet[0]);
        assertTrue(statusSet[0]);
        return stringWriter.getBuffer().toString().trim();
    }
    List<Product> callServletWithValidationAndGetItems() {
        return getProductsFromResponce(callServletWithValidationAndGetResponce());
    }

    abstract void runSupport();

    abstract List<Product> getProductsFromResponce(String htmlResponse);
}

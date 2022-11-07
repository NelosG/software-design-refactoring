package ru.akirakozov.sd.refactoring.servlet;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GetProductTest {
    private static String dbUrl;
    private static Path directory;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private void executeUpdate(String sql) {
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            Statement statement = connection.createStatement();
            statement.executeUpdate(sql);
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void initDb() {
        try {
            directory = Files.createTempDirectory(GetProductTest.class.getSimpleName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        dbUrl = "jdbc:sqlite:" + directory.resolve("tmp.db");
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

    @Before
    public void initTestDb() {
        executeUpdate("DROP TABLE IF EXISTS PRODUCT");
        executeUpdate("CREATE TABLE PRODUCT" +
                "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                " NAME           TEXT    NOT NULL, " +
                " PRICE          INT     NOT NULL)");
    }

    private void addProduct(Product product) {
        executeUpdate(String.format("INSERT INTO PRODUCT(NAME, PRICE) VALUES ('%s', %d)", product.name, product.price));
    }

    private List<Product> callServletWithValidationAndGetItems() {
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

        try {
            new GetProductsServlet(dbUrl).doGet(request, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertTrue(contentTypeSet[0]);
        assertTrue(statusSet[0]);

        return parseAndValidateHtml(stringWriter.getBuffer().toString().trim());
    }

    private List<Product> parseAndValidateHtml(String htmlResponse) {
        htmlResponse = htmlResponse.replaceAll("(\r)?\n", "");

        assertTrue(htmlResponse.startsWith("<html>"));
        assertTrue(htmlResponse.endsWith("</html>"));

        htmlResponse = htmlResponse.substring("<html>".length(), htmlResponse.length() - "</html>".length()).trim();
        assertTrue(htmlResponse.startsWith("<body>"));
        assertTrue(htmlResponse.endsWith("</body>"));

        htmlResponse = htmlResponse.substring("<body>".length(), htmlResponse.length() - "</body>".length()).trim();

        if (htmlResponse.isEmpty())
            return Collections.emptyList();

        List<Product> result = new ArrayList<>();
        for (String item : htmlResponse.split("</br>")) {
            String[] nameAndPrice = item.split("\t");
            result.add(new Product(nameAndPrice[0], Integer.parseInt(nameAndPrice[1])));
        }

        return result;
    }

    @Test
    public void testEmpty() {
        List<Product> products = callServletWithValidationAndGetItems();
        assertTrue(products.isEmpty());
    }

    @Test
    public void basicTest() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("a", 1));
        products.add(new Product("b", 2));
        products.add(new Product("c", 3));
        products.forEach(this::addProduct);

        List<Product> returnedProducts = callServletWithValidationAndGetItems();
        assertEquals(products, returnedProducts);
    }

    @Test
    public void randomTest() {
        List<Product> products = new ArrayList<>();

        for (int i = 0; i < 1000; i++)
            products.add(new Product(
                    UUID.randomUUID().toString(),
                    ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE)
            ));

        products.forEach(this::addProduct);

        List<Product> returnedProducts = callServletWithValidationAndGetItems();
        assertEquals(products, returnedProducts);
    }
}
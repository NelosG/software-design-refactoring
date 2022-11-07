package ru.akirakozov.sd.refactoring;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import ru.akirakozov.sd.refactoring.servlet.AddProductServlet;
import ru.akirakozov.sd.refactoring.servlet.GetProductsServlet;
import ru.akirakozov.sd.refactoring.servlet.QueryServlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

/**
 * @author akirakozov
 */
public class Main {
    public static void main(String[] args) throws Exception {
        final Properties properties = new Properties();
        properties.load(Main.class.getResourceAsStream("/aplication.properties"));
        
        String dbConnectionUrl = properties.getProperty("connection_url");
        String serverPortString = properties.getProperty("connection_url");
        int serverPort = Integer.parseInt(serverPortString);

        try (Connection c = DriverManager.getConnection(dbConnectionUrl)) {
            String sql = "CREATE TABLE IF NOT EXISTS PRODUCT" +
                    "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " PRICE          INT     NOT NULL)";
            Statement stmt = c.createStatement();

            stmt.executeUpdate(sql);
            stmt.close();
        }

        Server server = new Server(serverPort);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        context.addServlet(new ServletHolder(new AddProductServlet(dbConnectionUrl)), "/add-product");
        context.addServlet(new ServletHolder(new GetProductsServlet(dbConnectionUrl)),"/get-products");
        context.addServlet(new ServletHolder(new QueryServlet(dbConnectionUrl)),"/query");

        server.start();
        server.join();
    }
}

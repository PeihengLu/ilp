package uk.ac.ed.inf;

import java.sql.*;

public class DatabaseUtils {
    /** the address of the database server */
    public final String server;

    /**
     * construct a DatabaseUtils object with machine name, port and name of database, and create new
     * deliveries and flightpath tables
     * @param name machine name of the database server
     * @param port port to access database
     * @param dbName name of the database
     */
    public DatabaseUtils(String name, String port, String dbName) {
        this.server = "jdbc:derby://" + name + ":" + port + "/" + dbName;
        try {
            Connection connection = DriverManager.getConnection(this.server);
            Statement statement = connection.createStatement();

            // drop deliveries and flightpath tables if they already exist
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, null, "DELIVERIES", null);
            if (resultSet.next()) statement.execute("drop table deliveries");

            resultSet = databaseMetaData.getTables(null, null, "FLIGHTPATH", null);
            if (resultSet.next()) statement.execute("drop table flightpath");

            // create new deliveries and flightpath tables
            statement.execute(
                    "create table deliveries(" +
                            "orderNo char(8) ," +
                            "deliveredTo varchar(19) ," +
                            "costInPence int)"
            );
            statement.execute(
                    "create table flightpath(" +
                            "orderNo char(8) ," +
                            "fromLongitude double ," +
                            "fromLatitude double ," +
                            "angle integer ," +
                            "toLongitude double ," +
                            "toLatitude double)"
            );
        } catch (SQLException e) {
            System.err.println("Problem with db server connection");
            System.err.println(e.getMessage());
        }

    }

    public String getOrders(String date) {
        try {
            Connection connection = DriverManager.getConnection(this.server);
        } catch (SQLException e) {
            System.err.println("Problem with db server connection");
        }

        return null;
    }
}

package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
            createTables();
        } catch (SQLException e) {
            System.err.println("Problem with db server connection");
            System.err.println(e.getMessage());
        }
    }

    /**
     * create new deliveries and flightpath tables
     * @throws SQLException
     */
    private void createTables() throws SQLException {
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
    }


    /**
     *  retrieve the orders from database server
     * @param date the data of order
     * @return a list of lists containing order number and delivery address
     */
    public List<String[]> retrieveOrders(String date) {
        List<String[]> orderInfo = new ArrayList<>();

        try {
            Connection connection = DriverManager.getConnection(this.server);
            final String orderQuery = "select * from orders where deliveryDate=(?)";
            PreparedStatement preparedStatement = connection.prepareStatement(orderQuery);
            preparedStatement.setString(1, date);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                // store the order number and delivery address of one order
                String[] order = new String[2];
                order[0] = resultSet.getString("orderNo");
                order[1] = resultSet.getString("deliverTo");
                orderInfo.add(order);
            }
        } catch (SQLException ex) {
            System.err.println("Problem with db server connection");
            System.err.println(ex.getMessage());
            return null;
        }

        return orderInfo;
    }


    /**
     * get the items to deliver for an order
     * @param orderNo order number of the order
     * @return the items in that order
     */
    public List<String> getItems(String orderNo) {
        List<String> items = new ArrayList<>();

        try {
            Connection connection = DriverManager.getConnection(this.server);
            final String itemQuery = "select * from orderDetails where orderNo=(?)";
            PreparedStatement preparedStatement = connection.prepareStatement(itemQuery);
            preparedStatement.setString(1, orderNo);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                items.add(resultSet.getString("item"));
            }
        } catch (SQLException ex) {
            System.err.println("Problem with db server connection");
            System.err.println(ex.getMessage());
            return null;
        }

        return items;
    }


    /**
     * write into flightPath table
     * @param orderNo the order number of the order currently delivered by the drone
     * @param from the starting location of the drone
     * @param angle angle the drone is moving towards
     * @param to the next location of the drone
     */
    public boolean storePath(String orderNo, LongLat from, int angle, LongLat to) {
        try {
            Connection connection = DriverManager.getConnection(server);
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "insert into flightpath values (?, ?, ?, ?, ?, ?)"
            );

            preparedStatement.setString(1, orderNo);
            preparedStatement.setDouble(2, from.lng);
            preparedStatement.setDouble(3, from.lat);
            preparedStatement.setInt(4, angle);
            preparedStatement.setDouble(5, to.lng);
            preparedStatement.setDouble(6, to.lat);

            preparedStatement.execute();
        } catch (SQLException ex) {
            System.err.println("Problem with db server connection");
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }


    /**
     * write into the deliveries table
     * @param orderNo the order number of the order being delivered
     * @param deliveredTo the w3w location of the delivery address
     * @param cost the delivery cost in pence
     * @return true if no errors occurred, false otherwise
     */
    public boolean storeOrder(String orderNo, String deliveredTo, int cost) {
        try {
            Connection connection = DriverManager.getConnection(server);
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "insert into deliveries values (?, ?, ?)"
            );

            preparedStatement.setString(1, orderNo);
            preparedStatement.setString(2, deliveredTo);
            preparedStatement.setInt(3, cost);

            preparedStatement.execute();
        } catch (SQLException ex) {
            System.err.println("Problem with db server connection");
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }
}

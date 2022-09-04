import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {
    private static final String filePath = "C:\\Users\\Nick\\IdeaProjects\\CustomerSearchStat\\target\\input.json";

    public static void main(String[] args) throws SQLException {
        System.out.println(Charset.defaultCharset().displayName());

        final String url = "jdbc:postgresql://127.0.0.1:5432/shop_db";
        final String user = "postgres";
        final String password = "0000";


        try {
            FileReader reader = new FileReader(filePath);

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

            JSONArray arr = (JSONArray) jsonObject.get("criterias");

            for (Object o : arr) {
                JSONObject type = (JSONObject) o;

                String name = (String) type.get("lastName");
                if (name != null) {
                    System.out.println();
                    try {
                        Connection connection = DriverManager.getConnection(url, user, password);
                        getNames(connection, name);
                    } catch (Exception e) {
                        throw new Exception(e);
                    }
                }
                name = (String) type.get("productName");
                if (name != null) {
                    System.out.println();
                    try {
                        long count = (Long) type.get("minTimes");
                        Connection connection = DriverManager.getConnection(url, user, password);
                        getByGoods(connection, name, count);
                    } catch (Exception e) {
                        throw new Exception(e);
                    }
                }
                Double test = (Double) type.get("minExpenses");
                if (test != null) {
                    System.out.println();
                    try {
                        double d = 0;
                        Object obj = type.get("minExpenses");
                        if (obj instanceof Number) {
                            d = ((Number) obj).doubleValue();
                        }
                        double first = d;
                        obj = type.get("maxExpenses");
                        if (obj instanceof Number) {
                            d = ((Number) obj).doubleValue();
                        }
                        double second = d;
                        Connection connection = DriverManager.getConnection(url, user, password);
                        getByRange(connection, first, second);
                    } catch (Exception e) {
                        throw new Exception(e);
                    }
                }
                Long test2 = (Long) type.get("badCustomers");
                if (test2 != null) {
                    System.out.println();
                    try {
                        Connection connection = DriverManager.getConnection(url, user, password);
                        getBadCustomers(connection, test2);
                    } catch (Exception e) {
                        throw new Exception(e);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void getNames(Connection connection, String lastName) throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT last_name, first_name FROM customers WHERE last_name = (?);")) {
            preparedStatement.setString(1, lastName);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<String> last = new ArrayList<>();
            final ArrayList<String> first = new ArrayList<>();
            while (resultSet.next()) {
                last.add(resultSet.getString(1));
                first.add(resultSet.getString(2));
            }
            for (int i = 0; i < last.size(); i++) {
                System.out.println(last.get(i) + " " + first.get(i));
            }
        } finally {
            connection.close();
        }

    }

    public static void getByGoods(Connection connection, String Goods, long count) throws SQLException {

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("select customers.last_name, customers.first_name " +
                             "from customers " +
                             "WHERE (select count(*) " +
                             "       from purchases " +
                             "                join goods on purchases.goods_id = goods.goods_id " +
                             "       where goods.product_name = (?) " +
                             "         and customers.customer_id = purchases.customer_id) >= (?);")) {
            preparedStatement.setString(1, Goods);
            preparedStatement.setLong(2, count);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<String> last = new ArrayList<>();
            final ArrayList<String> first = new ArrayList<>();
            while (resultSet.next()) {
                last.add(resultSet.getString(1));
                first.add(resultSet.getString(2));
            }
            for (int i = 0; i < last.size(); i++) {
                System.out.println(last.get(i) + " " + first.get(i));
            }
        } finally {
            connection.close();
        }

    }

    public static void getByRange(Connection connection, double low, double high) throws SQLException {

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("select customers.last_name, customers.first_name " +
                             "from customers " +
                             "WHERE (select sum(goods.price) " +
                             "       from purchases " +
                             "                join goods on purchases.goods_id = goods.goods_id " +
                             "       where customers.customer_id = purchases.customer_id) between (?) and (?);")) {
            preparedStatement.setDouble(1, low);
            preparedStatement.setDouble(2, high);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<String> last = new ArrayList<>();
            final ArrayList<String> first = new ArrayList<>();
            while (resultSet.next()) {
                last.add(resultSet.getString(1));
                first.add(resultSet.getString(2));
            }
            for (int i = 0; i < last.size(); i++) {
                System.out.println(last.get(i) + " " + first.get(i));
            }
        } finally {
            connection.close();
        }

    }

    public static void getBadCustomers(Connection connection, long count) throws SQLException {

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("select customers.last_name, " +
                             "       customers.first_name, " +
                             "       (select count(*) " +
                             "        from purchases " +
                             "        where customers.customer_id = purchases.customer_id) " +
                             "from customers " +
                             "order by 3 " +
                             "LIMIT (?);")) {
            preparedStatement.setDouble(1, count);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final ArrayList<String> last = new ArrayList<>();
            final ArrayList<String> first = new ArrayList<>();
            while (resultSet.next()) {
                last.add(resultSet.getString(1));
                first.add(resultSet.getString(2));
            }
            for (int i = 0; i < last.size(); i++) {
                System.out.println(last.get(i) + " " + first.get(i));
            }
        } finally {
            connection.close();
        }

    }

}

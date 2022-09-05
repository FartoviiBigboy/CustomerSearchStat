import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

            if (args[0].equals("search")) {
                firstPart(reader, url, user, password);
            } else if (args[0].equals("stat")) {
                secondPart(reader, url, user, password);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void firstPart(FileReader reader, String url, String user, String password) throws Exception {
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
    }

    public static void secondPart(FileReader reader, String url, String user, String password) throws Exception {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

        LocalDate startDate = LocalDate.parse((String) jsonObject.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) jsonObject.get("endDate"));
        System.out.println(getCountOfWorkingDays(startDate, endDate));


        final ArrayList<Integer> cutomerId = new ArrayList<>();
        final ArrayList<String> customerLastFirstName = new ArrayList<>();

        Connection connection = DriverManager.getConnection(url, user, password);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT customer_id, last_name||' '||first_name FROM customers;")) {
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                cutomerId.add(resultSet.getInt(1));
                customerLastFirstName.add(resultSet.getString(2));
            }
            for (int i = 0; i < cutomerId.size(); i++) {
                System.out.println(customerLastFirstName.get(i));
                PreparedStatement preparedStatementInner = connection.prepareStatement("select goods.product_name, " +
                        "       (select count(*) * goods.price " +
                        "        from purchases " +
                        "                 join goods g on purchases.goods_id = g.goods_id " +
                        "        where customer_id = (?) " +
                        "          and purchases.goods_id = goods.goods_id " +
                        "          and purchases.purchase_time between (?) and (?)) " +
                        "from goods " +
                        "where (select count(*) " +
                        "       from purchases " +
                        "       where purchases.customer_id = (?) " +
                        "         and purchases.goods_id = goods.goods_id " +
                        "         and purchases.purchase_time between (?) and (?)) > 0;");
                preparedStatementInner.setInt(1, cutomerId.get(i));
                preparedStatementInner.setInt(4, cutomerId.get(i));
                preparedStatementInner.setDate(2, Date.valueOf(startDate.toString()));
                preparedStatementInner.setDate(5, Date.valueOf(startDate.toString()));
                preparedStatementInner.setDate(3, Date.valueOf(endDate.toString()));
                preparedStatementInner.setDate(6, Date.valueOf(endDate.toString()));

                final ResultSet resultSetInner = preparedStatementInner.executeQuery();
                while (resultSetInner.next()) {
                    System.out.println(resultSetInner.getString(1) + " " + resultSetInner.getDouble(2));
                }
            }
        } finally {
            connection.close();
        }
    }

    public static int getCountOfWorkingDays(LocalDate startDate, LocalDate endDate) throws Exception {
        int result = 0;
        while (!startDate.isEqual(endDate)) {
            if (!(startDate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || startDate.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                result++;
            }
            startDate = startDate.plusDays(1);
        }

        return result;
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

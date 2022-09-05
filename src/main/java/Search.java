import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Search implements Repository<Customer> {
    private final String url;
    private final String user;
    private final String password;

    public Search(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void findByJSON(String inputJsonFileName, String outputJsonFileName) {
        ArrayList<Customer> Customers = null;
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            JSONArray listOfCriterias = getListOfCriterias(inputJsonFileName);


            for (Object type : listOfCriterias) {
                JSONObject jsonObject = (JSONObject) type;
                if (jsonObject.containsKey("lastName")) {
                    String lastName = jsonObject.get("lastName").toString();
                    PreparedStatement preparedStatement = getByNames(connection, lastName);
                    Customers = (ArrayList<Customer>) read(preparedStatement);
                }
                if (jsonObject.containsKey("productName")) {
                    String productName = jsonObject.get("productName").toString();
                    long minTimes = (long) jsonObject.get("minTimes");
                    PreparedStatement preparedStatement = getByGoods(connection, productName, minTimes);
                    Customers = (ArrayList<Customer>) read(preparedStatement);
                }
                if (jsonObject.containsKey("minExpenses")) {
                    double low = convertToDouble(jsonObject.get("minExpenses"));
                    double high = convertToDouble(jsonObject.get("maxExpenses"));
                    PreparedStatement preparedStatement = getByRange(connection, low, high);
                    Customers = (ArrayList<Customer>) read(preparedStatement);
                }
                if (jsonObject.containsKey("badCustomers")) {
                    long countOfBadCustomers = (long) jsonObject.get("badCustomers");
                    PreparedStatement preparedStatement = getBadCustomers(connection, countOfBadCustomers);
                    Customers = (ArrayList<Customer>) read(preparedStatement);
                }
                if (Customers != null) {
                    for (Customer customer : Customers) {
                        System.out.println(customer.getLastName() + " " + customer.getFirstName());
                    }
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("!");
        }

    }

    @Override
    public List<Customer> read(PreparedStatement preparedStatement) throws SQLException {
        ArrayList<Customer> Customers = new ArrayList<>();

        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Customers.add(new Customer(resultSet.getString(1), resultSet.getString(2)));
        }

        return Customers;
    }

    private JSONArray getListOfCriterias(String inputJsonFileName) throws IOException, ParseException {
        String criteriaName = "criterias";
        FileReader inputJsonFile = new FileReader(inputJsonFileName);
        JSONParser jsonParser = new JSONParser();

        JSONObject jsonObject = (JSONObject) jsonParser.parse(inputJsonFile);

        return (JSONArray) jsonObject.get(criteriaName);
    }

    private PreparedStatement getByNames(Connection connection, String lastName) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_NAMES.QUERY);
        preparedStatement.setString(1, lastName);
        return preparedStatement;
    }

    private PreparedStatement getByGoods(Connection connection, String Goods, long count) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_BY_GOODS.QUERY);
        preparedStatement.setString(1, Goods);
        preparedStatement.setLong(2, count);
        return preparedStatement;
    }

    private PreparedStatement getByRange(Connection connection, double low, double high) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_BY_RANGE.QUERY);
        preparedStatement.setDouble(1, low);
        preparedStatement.setDouble(2, high);
        return preparedStatement;
    }

    private PreparedStatement getBadCustomers(Connection connection, long count) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_BAD_CUSTOMERS.QUERY);
        preparedStatement.setDouble(1, count);
        return preparedStatement;
    }

    private double convertToDouble(Object input) {
        double result = -1;
        if (input instanceof Number) {
            result = ((Number) input).doubleValue();
        }
        return result;
    }

    private enum StatSQL {
        GET_NAMES("SELECT last_name, first_name " +
                "FROM customers " +
                "WHERE last_name = (?);"),

        GET_BY_GOODS("select customers.last_name, customers.first_name " +
                "from customers " +
                "WHERE (select count(*) " +
                "       from purchases " +
                "                join goods on purchases.goods_id = goods.goods_id " +
                "       where goods.product_name = (?) " +
                "         and customers.customer_id = purchases.customer_id) >= (?);"),

        GET_BY_RANGE("select customers.last_name, customers.first_name " +
                "from customers " +
                "WHERE (select sum(goods.price) " +
                "       from purchases " +
                "                join goods on purchases.goods_id = goods.goods_id " +
                "       where customers.customer_id = purchases.customer_id) between (?) and (?);"),

        GET_BAD_CUSTOMERS("select customers.last_name, " +
                "       customers.first_name, " +
                "       (select count(*) " +
                "        from purchases " +
                "        where customers.customer_id = purchases.customer_id) " +
                "from customers " +
                "order by 3 " +
                "LIMIT (?);");

        final String QUERY;

        StatSQL(String QUERY) {
            this.QUERY = QUERY;
        }
    }

}

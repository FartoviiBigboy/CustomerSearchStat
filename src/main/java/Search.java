import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class Search implements Repository<Customer> {
    private final String url;
    private final String user;
    private final String password;

    private final HashMap<String, IResponseHandler> strategyHandlers;

    public Search(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;

        strategyHandlers = new HashMap<>();
        strategyHandlers.put("lastName", new getByNamesHandler());
        strategyHandlers.put("productName", new getByGoodsHandler());
        strategyHandlers.put("minExpenses", new getByRangeHandler());
        strategyHandlers.put("badCustomers", new getBadCustomersHandler());
    }

    public void findByJSON(String inputJsonFileName, String outputJsonFileName) throws IOException {
        ArrayList<ArrayList<Customer>> customers = new ArrayList<>();
        Path path = Paths.get(outputJsonFileName);

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            JSONArray listOfCriterias = getListOfCriterias(inputJsonFileName);

            for (Object type : listOfCriterias) {
                JSONObject jsonObject = (JSONObject) type;

                for (Map.Entry<String, IResponseHandler> entry : strategyHandlers.entrySet()) {
                    String key = entry.getKey();
                    if (jsonObject.containsKey(key)) {
                        IResponseHandler value = entry.getValue();
                        customers.add((ArrayList<Customer>) value.handleResponse(connection, jsonObject));
                    }
                }
            }

            JSONObject result = generateOutput(listOfCriterias, customers);
            Files.write(path, result.toJSONString().getBytes());

        } catch (Exception e) {
            JSONObject result = generateError(e);
            Files.write(path, result.toJSONString().getBytes());
        }

    }

    private JSONArray getListOfCriterias(String inputJsonFileName) throws IOException, ParseException {
        String criteriaName = "criterias";
        FileReader inputJsonFile = new FileReader(inputJsonFileName);
        JSONParser jsonParser = new JSONParser();

        JSONObject jsonObject = (JSONObject) jsonParser.parse(inputJsonFile);

        return (JSONArray) jsonObject.get(criteriaName);
    }

    private class getByNamesHandler implements IResponseHandler<Customer> {
        @Override
        public List<Customer> handleResponse(Connection connection, JSONObject parameter) throws SQLException {
            String lastName = parameter.get("lastName").toString();

            PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_NAMES.QUERY);
            preparedStatement.setString(1, lastName);

            return read(preparedStatement);
        }
    }

    private class getByGoodsHandler implements IResponseHandler<Customer> {
        @Override
        public List<Customer> handleResponse(Connection connection, JSONObject parameter) throws SQLException {
            String productName = parameter.get("productName").toString();
            long minTimes = (long) parameter.get("minTimes");

            PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_BY_GOODS.QUERY);
            preparedStatement.setString(1, productName);
            preparedStatement.setLong(2, minTimes);

            return read(preparedStatement);
        }
    }

    private class getByRangeHandler implements IResponseHandler<Customer> {
        @Override
        public List<Customer> handleResponse(Connection connection, JSONObject parameter) throws SQLException {
            double low = convertToDouble(parameter.get("minExpenses"));
            double high = convertToDouble(parameter.get("maxExpenses"));

            PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_BY_RANGE.QUERY);
            preparedStatement.setDouble(1, low);
            preparedStatement.setDouble(2, high);

            return read(preparedStatement);
        }
    }

    private class getBadCustomersHandler implements IResponseHandler<Customer> {
        @Override
        public List<Customer> handleResponse(Connection connection, JSONObject parameter) throws SQLException {
            long countOfBadCustomers = (long) (parameter.get("badCustomers"));

            PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_BAD_CUSTOMERS.QUERY);
            preparedStatement.setDouble(1, countOfBadCustomers);

            return read(preparedStatement);
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

    private double convertToDouble(Object input) {
        double result = -1;
        if (input instanceof Number) {
            result = ((Number) input).doubleValue();
        }
        return result;
    }

    private JSONObject generateOutput(JSONArray listOfCriterias, ArrayList<ArrayList<Customer>> customers) {
        JSONObject result = new JSONObject();

        JSONArray outerArray = new JSONArray();
        for (int i = 0; i < listOfCriterias.size(); i++) {
            JSONObject outerInformation = new JSONObject();

            JSONArray innerArray = new JSONArray();
            for (int j = 0; j < customers.get(i).size(); j++) {
                JSONObject temp = new JSONObject();

                temp.put("lastName", customers.get(i).get(j).getLastName());
                temp.put("firstName", customers.get(i).get(j).getFirstName());
                innerArray.add(temp);
            }

            outerInformation.put("criteria", listOfCriterias.get(i));
            outerInformation.put("results", innerArray);
            outerArray.add(outerInformation);
        }

        result.put("type", "search");
        result.put("results", outerArray);

        return result;
    }

    private JSONObject generateError(Exception e) {
        JSONObject result = new JSONObject();

        result.put("type", "error");
        result.put("results", e.fillInStackTrace().toString());

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

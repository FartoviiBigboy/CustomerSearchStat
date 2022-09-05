import java.io.FileReader;
import java.nio.charset.Charset;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {
    public static void main(String[] args) throws SQLException {
        System.out.println(Charset.defaultCharset().displayName());

        if (true) {
            final String url = "jdbc:postgresql://127.0.0.1:5432/shop_db";
            final String user = "postgres";
            final String password = "0000";
            if (args[0].equals("search")) {
                Search searcher = new Search(url, user, password);
                searcher.findByJSON(args[1], args[2]);
                //firstPart(reader, url, user, password);
            } else if (args[0].equals("stat")) {
                Statistic statistician = new Statistic(url, user, password);
                statistician.getStatisticByJSON(args[1], args[2]);
                //secondPart(reader, url, user, password);
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

}

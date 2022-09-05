import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class Statistic implements Repository<BuyerWithIdAndCheck> {
    private final String url;
    private final String user;
    private final String password;

    public Statistic(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void getStatisticByJSON(String inputJsonFileName, String outputJsonFileName) throws IOException {
        JSONObject result;

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            JSONParser jsonParser = new JSONParser();
            FileReader inputJsonFile = new FileReader(inputJsonFileName);
            JSONObject jsonObject = (JSONObject) jsonParser.parse(inputJsonFile);

            LocalDate startDate = LocalDate.parse((String) jsonObject.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) jsonObject.get("endDate"));
            int countOfWorkingDays = getCountOfWorkingDays(startDate, endDate);

            PreparedStatement preparedStatement = connection.prepareStatement(StatSQL.GET_NAMES_WITH_ID.QUERY);
            preparedStatement.setDate(1, Date.valueOf(startDate.toString()));
            preparedStatement.setDate(2, Date.valueOf(endDate.toString()));
            ArrayList<BuyerWithIdAndCheck> buyers = (ArrayList<BuyerWithIdAndCheck>) read(preparedStatement);

            for (BuyerWithIdAndCheck buyer : buyers) {
                PreparedStatement preparedStatementInner = connection.prepareStatement(StatSQL.GET_GOODS.QUERY);
                preparedStatementInner.setInt(1, buyer.getBuyerId());
                preparedStatementInner.setInt(4, buyer.getBuyerId());
                preparedStatementInner.setDate(2, Date.valueOf(startDate.toString()));
                preparedStatementInner.setDate(5, Date.valueOf(startDate.toString()));
                preparedStatementInner.setDate(3, Date.valueOf(endDate.toString()));
                preparedStatementInner.setDate(6, Date.valueOf(endDate.toString()));

                final ResultSet resultSetInner = preparedStatementInner.executeQuery();
                ArrayList<GeneralPurchase> purchases = new ArrayList<>();
                while (resultSetInner.next()) {
                    purchases.add(new GeneralPurchase(resultSetInner.getString(1), resultSetInner.getDouble(2)));
                }

                buyer.setTotalPurchasePrice(purchases);
            }

            result = generateOutput(countOfWorkingDays, buyers);
        } catch (Exception e) {
            result = generateError(e);
        }

        Files.write(Paths.get(outputJsonFileName), result.toJSONString().getBytes());
    }


    private int getCountOfWorkingDays(LocalDate startDate, LocalDate endDate) {
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

    @Override
    public List<BuyerWithIdAndCheck> read(PreparedStatement preparedStatement) throws SQLException {
        ArrayList<BuyerWithIdAndCheck> result = new ArrayList<>();

        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            result.add(new BuyerWithIdAndCheck(
                    resultSet.getInt(1),
                    resultSet.getString(2),
                    resultSet.getDouble(3)));
        }

        return result;
    }

    private JSONObject generateOutput(int countOfWorkingDays, ArrayList<BuyerWithIdAndCheck> buyers) {
        JSONObject result = new JSONObject();

        JSONArray outerArray = new JSONArray();
        for (int i = 0; i < buyers.size(); i++) {
            JSONObject outerInformation = new JSONObject();

            JSONArray innerArray = new JSONArray();
            for (int j = 0; j < buyers.get(i).getPurchases().size(); j++) {
                JSONObject temp = new JSONObject();

                temp.put("name", buyers.get(i).getPurchases().get(j).getGoodsName());
                temp.put("expenses", buyers.get(i).getPurchases().get(j).getSum());
                innerArray.add(temp);
            }

            outerInformation.put("name", buyers.get(i).getFullName());
            outerInformation.put("purchases", innerArray);
            outerInformation.put("totalExpenses", buyers.get(i).getTotalPurchasePrice());
            outerArray.add(outerInformation);
        }

        result.put("type", "stat");
        result.put("totalDays", countOfWorkingDays);
        result.put("customers", outerArray);
        result.put("totalExpenses", getTotalExpenses(buyers));
        result.put("avgExpenses", getAvgExpenses(buyers));

        return result;
    }

    private double getTotalExpenses(ArrayList<BuyerWithIdAndCheck> buyers) {
        double sum = 0;
        for (BuyerWithIdAndCheck buyer : buyers) {
            sum += buyer.getTotalPurchasePrice();
        }
        return sum;
    }

    private double getAvgExpenses(ArrayList<BuyerWithIdAndCheck> buyers) {
        return getTotalExpenses(buyers) / (double) buyers.size();
    }

    private JSONObject generateError(Exception e) {
        JSONObject result = new JSONObject();

        result.put("type", "error");
        result.put("results", e.fillInStackTrace().toString());

        return result;
    }

    private enum StatSQL {
        GET_NAMES_WITH_ID("select customers.customer_id, " +
                "       last_name || ' ' || first_name, " +
                "       coalesce((select sum(goods.price) " +
                "                 from goods " +
                "                          join purchases p on goods.goods_id = p.goods_id " +
                "                 where customers.customer_id = p.customer_id " +
                "                   and p.purchase_time between (?) and (?)), 0) " +
                "from customers " +
                "group by customer_id " +
                "order by 3 desc;"),

        GET_GOODS("select goods.product_name, " +
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

        final String QUERY;

        StatSQL(String QUERY) {
            this.QUERY = QUERY;
        }
    }

}

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

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println(Charset.defaultCharset().displayName());

        if (true) {
            final String url = "jdbc:postgresql://127.0.0.1:5432/shop_db";
            final String user = "postgres";
            final String password = "0000";
            if (args[0].equals("search")) {
                Search searcher = new Search(url, user, password);
                searcher.findByJSON(args[1], args[2]);
            } else if (args[0].equals("stat")) {
                Statistic statistician = new Statistic(url, user, password);
                statistician.getStatisticByJSON(args[1], args[2]);
            }
        }

    }

}

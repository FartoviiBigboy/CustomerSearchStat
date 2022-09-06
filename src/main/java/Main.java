import SearchEngine.Search;
import StatisticEngine.Statistic;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        //System.out.println(Charset.defaultCharset().displayName());

        if (isReadyToStart(args)) {
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
        } else {
            System.out.println("Ошибка при задании аргументов командной строки!");
        }

    }

    private static boolean isReadyToStart(String[] args) {
        boolean result = false;
        if (args.length == 3 && (args[0].equals("search") || args[0].equals("stat"))) {
            File f = new File(args[1]);
            if(f.exists() && !f.isDirectory()) {
                result = true;
            }
        }
        return result;
    }

}

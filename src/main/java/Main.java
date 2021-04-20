import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {

        ApiContextInitializer.init();

        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        TinkoffBot BrokTheBot = new TinkoffBot(botOptions);

        String userName = "admin";
        String password = "root";
        String connectionURL = "jdbc:mysql://localhost:3306/tinkoffbot";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }





        TelegramBotsApi botsApi = new TelegramBotsApi();

        try{
            botsApi.registerBot(BrokTheBot);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}

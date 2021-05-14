import lombok.SneakyThrows;

import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.models.orders.LimitOrder;
import ru.tinkoff.invest.openapi.models.orders.MarketOrder;
import ru.tinkoff.invest.openapi.models.orders.Operation;
import ru.tinkoff.invest.openapi.models.orders.Order;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import javax.ws.rs.core.Application;
import java.util.logging.Logger;


public class TinkoffBot extends TelegramLongPollingBot {

    private static final String TOKEN = "1707052474:AAH0bAaVVKwHwVJ2shJCIa4Gk4qoH9-v6sA";
    private static final String USERNAME = "brok_the_bot";

    private String TINTOKEN = "";

    String userName = "admin";
    String password = "root";
    String connectionURL = "jdbc:mysql://localhost:3306/tinkoffbot";

    public TinkoffBot(DefaultBotOptions options) {
        super(options);
    }

    public String getBotToken() {
        return TOKEN;
    }

    public String getBotUsername() {
        return USERNAME;
    }

    public ArrayList<String> messages = new ArrayList<>();
    public Integer MessageCounter = 0;
    public Integer TokenNumber = 0;
    public Integer PageNumber = 0;

    public Integer BuyNumber = 0;
    public Integer BuyLotNumber = 0;
    public Integer BuyPriceNumber = 0;
    public Integer SellNumber = 0;
    public Integer SellLotNumber = 0;
    public Integer SellPriceNumber = 0;


    public String Figi;
    public Integer Lots;
    public BigDecimal Price;


    int sandboxMode;

    public static Logger logger = Logger.getLogger(Application.class.toString());

    @SneakyThrows
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        if (update.getMessage() != null && update.getMessage().hasText()) {


            long chat_id = update.getMessage().getChatId();
            String str_chat_id = Long.toString(chat_id);
            String tokenToInsert;
            messages.add(update.getMessage().getText());
            MessageCounter++; //Шаг по истории запросов
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            SendMessage sendMessage = new SendMessage().setChatId(chat_id);




            try(Connection connection = DriverManager.getConnection(connectionURL, userName, password);
                Statement statement = connection.createStatement()){
                ResultSet resultSet = statement.executeQuery("SELECT Token FROM users WHERE chatId =" + str_chat_id + " AND Mode = " + sandboxMode);
                while(resultSet.next()){

                    String Token = resultSet.getString(1);
                    TINTOKEN = Token;
                }

            }
            catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(TINTOKEN, logger);
            OpenApi api = null;



            if (sandboxMode == 1) {
                api = factory.createSandboxOpenApiClient(Executors.newCachedThreadPool());
                ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();

            } else {
                api = factory.createOpenApiClient(Executors.newCachedThreadPool());
            }



            if (TokenNumber != 0 && MessageCounter == TokenNumber + 1 ) {

                tokenToInsert = messages.get(TokenNumber);

                try(Connection connection = DriverManager.getConnection(connectionURL, userName, password);
                    Statement statement = connection.createStatement()){

                    if(sandboxMode == 1) {
                        statement.executeUpdate("INSERT INTO users (chatId, Token, Mode) VALUES ('" + str_chat_id + "', '" + tokenToInsert + "', 1)");
                    }
                    else {
                        statement.executeUpdate("INSERT INTO users (chatId, Token, Mode) VALUES ('"+ str_chat_id +"', '" + tokenToInsert + "', 0)");

                    }

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

                execute(new SendMessage(str_chat_id, "Токен установлен"));
            }

            if (PageNumber != 0 && MessageCounter == PageNumber + 1 ) {

                if(Integer.parseInt(update.getMessage().getText()) > 162 || Integer.parseInt(update.getMessage().getText()) < 1) {
                    execute(new SendMessage(str_chat_id, "Такой страницы не существует"));
                }
                else {
                    ArrayList<String> brokList = new ArrayList<String>();

                    api.getMarketContext().getMarketStocks().get().instruments.forEach(element -> {
                        brokList.add(element.ticker + System.lineSeparator()
                                + element.name + System.lineSeparator()
                                + "FIGI: " + element.figi + System.lineSeparator()
                                + "Валюта: " + element.currency + System.lineSeparator()
                                + "Кол-во в лоте: " + element.lot + System.lineSeparator()
                                + "-------------------------------------------------------------"+ System.lineSeparator());

                    });


                    Integer numberOfElements = Integer.parseInt(update.getMessage().getText());

                    String startNumber = Integer.toString(numberOfElements * 10 - 10 + 1);
                    String endNumber = Integer.toString(numberOfElements * 10) ;

                    ArrayList<String> shortList = new ArrayList<String>();

                    shortList.add("Список акций");
                    shortList.add("С " + startNumber + " по " + endNumber + System.lineSeparator());
                    shortList.add("=============================" + System.lineSeparator());


                    for(int i = numberOfElements * 10 - 10; i <= numberOfElements * 10 - 1; i++) {
                        shortList.add(brokList.get(i));
                    }

                    String shortListStr = String.join(System.lineSeparator(), shortList);

                    execute(new SendMessage(str_chat_id, shortListStr));
                }


            }if (BuyNumber != 0 && MessageCounter == BuyNumber + 1 ) {
                Figi = messages.get(BuyNumber);
                execute(new SendMessage(str_chat_id, "Введи кол-во лотов:"));
                BuyLotNumber = MessageCounter;
            }
            if (BuyLotNumber != 0 && MessageCounter == BuyLotNumber + 1 ) {
                execute(new SendMessage(str_chat_id, "Введите желаемую стоимость:"));
                Lots = Integer.parseInt(messages.get(BuyLotNumber));
                BuyPriceNumber = MessageCounter;
            }
            if (BuyPriceNumber != 0 && MessageCounter == BuyPriceNumber + 1 ) {
                execute(new SendMessage(str_chat_id, "Покупка произведена успешно"));
                Price = BigDecimal.valueOf(Double.valueOf(messages.get(BuyPriceNumber)));
                api.getOrdersContext().placeLimitOrder(Figi, new LimitOrder(Lots, Operation.Buy, Price), api.getUserContext().getAccounts().get().accounts.get(0).brokerAccountId).get();
            }

            if (SellNumber != 0 && MessageCounter == SellNumber + 1 ) {
                Figi = messages.get(SellNumber);
                execute(new SendMessage(str_chat_id, "Введи кол-во лотов:"));
                SellLotNumber = MessageCounter;
            }
            if (SellLotNumber != 0 && MessageCounter == SellLotNumber + 1 ) {
                execute(new SendMessage(str_chat_id, "Введите желаемую стоимость:"));
                Lots = Integer.parseInt(messages.get(SellLotNumber));
                SellPriceNumber = MessageCounter;
                //api.getOrdersContext().placeMarketOrder(Figi, new MarketOrder(Lots, Operation.Buy), api.getUserContext().getAccounts().get().accounts.get(0).brokerAccountId).get();
            }
            if (SellPriceNumber != 0 && MessageCounter == SellPriceNumber + 1 ) {
                execute(new SendMessage(str_chat_id, "Продажа произведена успешно"));
                Price = BigDecimal.valueOf(Double.valueOf(messages.get(SellPriceNumber)));
                api.getOrdersContext().placeLimitOrder(Figi, new LimitOrder(Lots, Operation.Sell, Price), api.getUserContext().getAccounts().get().accounts.get(0).brokerAccountId).get();
            }

            if (update.getMessage().getText().toString().equals("/mode")) {
                try {
                    execute(sendInlineKeyBoardMessage(update.getMessage().getChatId()));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
            else if(update.getMessage().getText().toString().equals("/start") || update.getMessage().getText().toString().equals("/help")) {
                String startMessage = "Для начала выберите режим* при помощи команды /mode (По умолчанию установлен 'обычный') \n"
                        +System.lineSeparator()
                        + "Введите команду /help, чтобы снова получить это сообщение \n";

                execute(new SendMessage(str_chat_id, startMessage));
            }
            else if (update.getMessage().getText().toString().equals("/token")) {
                execute(new SendMessage(str_chat_id, "Введите Токен"));
                TokenNumber = MessageCounter;
            }
            else if (update.getMessage().getText().toString().equals("/list")) {
                execute(new SendMessage(str_chat_id, "Введите номер страницы(1 - 162):"));
                PageNumber = MessageCounter;
            }
            else if(update.getMessage().getText().toString().equals("/status")) {

                ArrayList<String> portfolioStatus = new ArrayList<>();

                api.getPortfolioContext().getPortfolio(api.getUserContext().getAccounts().get().accounts.get(0).brokerAccountId).get().positions.forEach(element -> {
                    portfolioStatus.add("Figi: " + element.figi + System.lineSeparator());
                    portfolioStatus.add(element.name + System.lineSeparator());
                    portfolioStatus.add("Количество: " + (element.balance).doubleValue() + System.lineSeparator());
                    if(sandboxMode == 0) {
                        portfolioStatus.add(element.averagePositionPrice + System.lineSeparator());
                    }
                    portfolioStatus.add("=======================" + System.lineSeparator());
                });

                ArrayList<String> shortList = new ArrayList<String>();

                String numberofShares = Integer.toString(portfolioStatus.size() / 5) ;

                shortList.add("ВАШ ПОРТФЕЛЬ");
                shortList.add("На данный момент у вас в портфеле " + numberofShares + " акций(я/и):" + System.lineSeparator());
                shortList.add("=======================" + System.lineSeparator());

                String PortfolioStatusList = String.join(System.lineSeparator(), portfolioStatus);

                shortList.add(PortfolioStatusList);

                String finalStatus = String.join(System.lineSeparator(), shortList);

                TINTOKEN = "";

                execute(new SendMessage(str_chat_id, finalStatus));
            }
            else if (update.getMessage().getText().toString().equals("/balance")) {
                ArrayList<String> walletStatus = new ArrayList<>();

                api.getPortfolioContext().getPortfolioCurrencies(api.getUserContext().getAccounts().get().accounts.get(0).brokerAccountId).get().currencies.forEach(element -> {
                    walletStatus.add("" + element.currency + ": " + (element.balance).doubleValue() + System.lineSeparator());
                    walletStatus.add("=======================" + System.lineSeparator());
                });

                ArrayList<String> shortList = new ArrayList<String>();

                shortList.add("ВАШ БАЛАНС");
                shortList.add("На данный момент у вас на счету:       " + System.lineSeparator());
                shortList.add("=======================" + System.lineSeparator());

                String walletStatusList = String.join(System.lineSeparator(), walletStatus);

                shortList.add(walletStatusList);

                String finalStatus = String.join(System.lineSeparator(), shortList);

                TINTOKEN = "";

                execute(new SendMessage(str_chat_id, finalStatus));
            }
            else if (update.getMessage().getText().toString().equals("/buy")) {
                execute(new SendMessage(str_chat_id, "Что вы хотите купить(FIGI)?"));
                BuyNumber = MessageCounter;
            }
            else if (update.getMessage().getText().toString().equals("/sell")) {
                execute(new SendMessage(str_chat_id, "Что вы хотите продать(FIGI)?"));
                SellNumber = MessageCounter;
            }
        }
        else if(update.hasCallbackQuery()) {
            sandboxMode = Integer.parseInt(update.getCallbackQuery().getData());

            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (sandboxMode == 1) {
                execute(new SendMessage(chatId, "Выбран режим песочницы \uD83C\uDFDD\uFE0F"));
            }else if (sandboxMode == 0) {
                execute(new SendMessage(chatId, "Выбран обычный режим торговли \uD83D\uDCB0"));
            }

        }

    }
    public static SendMessage sendInlineKeyBoardMessage(long chatId) {


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonSandbox = new InlineKeyboardButton();
        inlineKeyboardButtonSandbox.setText("Песочница \uD83C\uDFDD\uFE0F");
        inlineKeyboardButtonSandbox.setCallbackData("1");

        InlineKeyboardButton inlineKeyboardButtonDefault = new InlineKeyboardButton();
        inlineKeyboardButtonDefault.setText("Обычный \uD83D\uDCB0");
        inlineKeyboardButtonDefault.setCallbackData("0");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonSandbox);
        keyboardButtonsRow1.add(inlineKeyboardButtonDefault);


        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(rowList);
        return new SendMessage().setChatId(chatId).setText("Выберите режим:").setReplyMarkup(inlineKeyboardMarkup);
    }
}
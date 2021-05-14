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


            OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(TINTOKEN, logger);
            OpenApi api = null;

            try(Connection connection = DriverManager.getConnection(connectionURL, userName, password);
                Statement statement = connection.createStatement()){

            }
            catch (SQLException throwables) {
                throwables.printStackTrace();
            }



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
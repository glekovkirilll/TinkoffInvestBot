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


            if (sandboxMode == 1) {
                api = factory.createSandboxOpenApiClient(Executors.newCachedThreadPool());
                ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();

            } else {
                api = factory.createOpenApiClient(Executors.newCachedThreadPool());
            }

            
            if (update.getMessage().getText().toString().equals("/mode")) {
                try {
                    execute(sendInlineKeyBoardMessage(update.getMessage().getChatId()));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
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
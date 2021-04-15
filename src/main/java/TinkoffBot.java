import lombok.SneakyThrows;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.util.*;
import java.util.concurrent.Executors;





import javax.ws.rs.core.Application;
import java.util.logging.Logger;

public class TinkoffBot extends TelegramLongPollingBot {

    private static final String TOKEN = "1707052474:AAH0bAaVVKwHwVJ2shJCIa4Gk4qoH9-v6sA";
    private static final String USERNAME = "brok_the_bot";

    private String TINTOKEN = "";

    public TinkoffBot(DefaultBotOptions options) { super(options);}

    public String getBotToken() {return TOKEN;}
    public String getBotUsername() {return USERNAME;}

    public static Logger logger = Logger.getLogger(Application.class.toString());

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if(update.getMessage()!=null && update.getMessage().hasText()) {


            long chat_id = update.getMessage().getChatId();
            String str_chat_id = Long.toString(chat_id);


            //TINTOKEN = update.getMessage().getText();


            OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(TINTOKEN, logger);
            OpenApi api = null;

            boolean sandboxMode = false;
            if(sandboxMode) {
                api = factory.createSandboxOpenApiClient(Executors.newCachedThreadPool());
                ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();
            }
            else {
                api = factory.createOpenApiClient(Executors.newCachedThreadPool());
            }

            ArrayList<String> brokList = new ArrayList<String>();

            api.getMarketContext().getMarketStocks().get().instruments.forEach(element -> {
                brokList.add(element.figi + " " + element.name) ;
            });

            //String brokListStr = String.join(System.lineSeparator(), brokList);
            Integer numberOfElements = Integer.parseInt(update.getMessage().getText());

            for(int i = numberOfElements * 10 - 10; i < numberOfElements * 10 -1; i++) {
                execute(new SendMessage(str_chat_id, brokList.get(i )));
            }
        }
    }

}

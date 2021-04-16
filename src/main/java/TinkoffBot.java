import lombok.SneakyThrows;

import org.jetbrains.annotations.NotNull;
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

    public ArrayList<String> messages = new ArrayList<>();
    public Integer MessageCounter = 0;
    public Integer TokenNumber = 0;

    public static Logger logger = Logger.getLogger(Application.class.toString());

    @SneakyThrows
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        if(update.getMessage()!=null && update.getMessage().hasText()) {




            long chat_id = update.getMessage().getChatId();
            String str_chat_id = Long.toString(chat_id);
            messages.add(update.getMessage().getText());
            MessageCounter++; //Шаг по истории запросов




            OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(TINTOKEN, logger);
            OpenApi api = null;

            api = factory.createOpenApiClient(Executors.newCachedThreadPool());

            /*boolean sandboxMode = false;
            if(sandboxMode) {
                api = factory.createSandboxOpenApiClient(Executors.newCachedThreadPool());
                ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();
            }//пока что не рабочее что-то
            else {

            }*/

            TINTOKEN = messages.get(TokenNumber + 1);

            if (update.getMessage().getText().toString().equals("/token")) {
                execute(new SendMessage(str_chat_id, "Введите Токен"));
                TokenNumber = MessageCounter - 1;
            }
            else if (update.getMessage().getText().toString().equals("/array")) {
                execute(new SendMessage(str_chat_id, messages.get(MessageCounter - 2)));

            }
            else if(update.getMessage().getText().toString().equals("/start") || update.getMessage().getText().toString().equals("/help")) {

                String startMessage = "Чтобы установить свой токен Tinkoff используйте команду /token \n" +
                        "Если вы хотите узнать какие акции доступны к покупке введите число, которое будет являться страницей списка (на каждой странице по 10 акций) .\n" +
                        "Если вы хотите увидеть список акций в вашем портфеле напишите /status";

                execute(new SendMessage(str_chat_id, startMessage));
            }
            else if(update.getMessage().getText().toString().equals("/status")) {

                ArrayList<String> portfolioStatus = new ArrayList<>();

                api.getPortfolioContext().getPortfolio(api.getUserContext().getAccounts().get().accounts.get(0).brokerAccountId).get().positions.forEach(element -> {
                    portfolioStatus.add("Figi: " + element.figi + System.lineSeparator());
                    portfolioStatus.add(element.name + System.lineSeparator());
                    portfolioStatus.add("Количество: " + element.balance + System.lineSeparator());
                    portfolioStatus.add(element.averagePositionPrice + System.lineSeparator());
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


                execute(new SendMessage(str_chat_id, finalStatus));


            }
            else {
                ArrayList<String> brokList = new ArrayList<String>();

                api.getMarketContext().getMarketStocks().get().instruments.forEach(element -> {
                    brokList.add(element.figi + " " + element.name + System.lineSeparator()) ;
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


        }
    }

}

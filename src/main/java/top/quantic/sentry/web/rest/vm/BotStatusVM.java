package top.quantic.sentry.web.rest.vm;

public class BotStatusVM {

    private final String botId;
    private boolean isCreated = false;
    private boolean isLoggedIn = false;
    private boolean isReady = false;

    public BotStatusVM(String botId) {
        this.botId = botId;
    }

    public String getBotId() {
        return botId;
    }

    public boolean isCreated() {
        return isCreated;
    }

    public void setCreated(boolean created) {
        this.isCreated = created;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }
}

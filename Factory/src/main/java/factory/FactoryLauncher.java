package factory;

import javafx.application.Application;

public final class FactoryLauncher {
    private FactoryLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(FactoryApp.class, args);
    }
}

package csc311;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/** Loads factory-view.fxml + styles.css (both in the resources folder). */
public class FactoryApp extends Application {
    private FactoryController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(FactoryApp.class.getResource("factory-view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Objects.requireNonNull(FactoryApp.class.getResource("styles.css")).toExternalForm());
        controller = loader.getController();

        stage.setTitle("Factory Conveyor Belt Simulation");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) controller.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

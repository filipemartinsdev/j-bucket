package com.mybucket4j.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MyBucket4jApplication extends Application {
//    private final String VERSION = "1.0.0";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MyBucket4jApplication.class.getResource("login.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setScene(scene);
        stage.setTitle("MyBucket4j Login");
        stage.setWidth(400);
        stage.setHeight(600);
        stage.requestFocus();
        stage.toFront();
        stage.centerOnScreen();
        stage.show();
    }
}

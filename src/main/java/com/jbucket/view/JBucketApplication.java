package com.jbucket.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class JBucketApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(JBucketApplication.class.getResource("login.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setScene(scene);
        stage.setTitle("J-Bucket Login");
        stage.setWidth(400);
        stage.setHeight(600);
        stage.requestFocus();
        stage.toFront();
        stage.centerOnScreen();
        stage.getIcons().add(
                new Image(JBucketApplication.class.getResourceAsStream("/com/jbucket/view/icon.png"))
        );
        stage.show();
    }
}

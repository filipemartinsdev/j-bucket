package com.jbucket.controller;

import com.jbucket.model.BucketSession;
import com.jbucket.model.exception.InvalidCredentialsException;
import com.jbucket.model.service.BucketService;
import com.jbucket.view.JBucketApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

public class LoginController {
    private BucketService bucketService;

    @FXML
    private PasswordField accessKeyInput;

    @FXML
    private TextField bucketNameInput;

    @FXML
    private Button confirmBtn;

    @FXML
    private ComboBox<Region> regionComboBox;

    @FXML
    private CheckBox rememberCheckBox;

    @FXML
    private PasswordField secretKeyInput;

    private Preferences preferences = Preferences.userNodeForPackage(getClass());

    private final String PREFERENCE_BUCKET_NAME = "bucketName";
    private final String PREFERENCE_ACCESS_KEY = "accessKey";
    private final String PREFERENCE_SECRET_KEY = "secretKey";
    private final String PREFERENCE_REGION = "region";

    @FXML
    public void initialize() {
        bucketNameInput.setText(preferences.get(PREFERENCE_BUCKET_NAME, ""));
        accessKeyInput.setText(preferences.get(PREFERENCE_ACCESS_KEY, ""));
        secretKeyInput.setText(preferences.get(PREFERENCE_SECRET_KEY, ""));

        if (!bucketNameInput.getText().isEmpty()) {
            rememberCheckBox.setSelected(true);
        }

        List<Region> regions = Region.regions();
        List<Region> sortedRegions = new ArrayList<>(regions);
        sortedRegions.sort(Comparator.comparing(Region::id));

        regionComboBox.getItems().addAll(sortedRegions);
    }

    @FXML
    public void confirmLogin(ActionEvent event) throws IOException {
        try {
            validateInputs(
                    bucketNameInput,
                    accessKeyInput,
                    secretKeyInput,
                    regionComboBox
            );
        } catch (InvalidCredentialsException e) {
            return;
        }

        if (rememberCheckBox.isSelected()) {
            preferences.put(PREFERENCE_BUCKET_NAME, bucketNameInput.getText());
            preferences.put(PREFERENCE_ACCESS_KEY, accessKeyInput.getText());
            preferences.put(PREFERENCE_SECRET_KEY, secretKeyInput.getText());
        } else {
            preferences.remove(PREFERENCE_BUCKET_NAME);
            preferences.remove(PREFERENCE_ACCESS_KEY);
            preferences.remove(PREFERENCE_SECRET_KEY);
        }

        String bucketName = bucketNameInput.getText();
        String accessKey = accessKeyInput.getText();
        String secretKey = secretKeyInput.getText();
        Region region = regionComboBox.getValue();

        BucketSession session = new BucketSession(bucketName, accessKey, secretKey, region);

        BucketService bucketService = null;
        try {
            bucketService = new BucketService(session);
        } catch (Exception e) {
            showAlert();
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(JBucketApplication.class.getResource("index.fxml"));
        fxmlLoader.setController(new JBucketController(bucketService));

        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = (Stage) confirmBtn.getScene().getWindow();
        stage.hide();
        stage.setScene(scene);
        stage.setTitle("J-Bucket");
        stage.setWidth(1024);
        stage.setHeight(728);
        stage.setMinWidth(400);
        stage.setMinHeight(200);
        stage.centerOnScreen();
        stage.show();
    }

    private void validateInputs(TextField bucketName, PasswordField accessKey, PasswordField secretKey, ComboBox<Region> region) {
        if (
                bucketName == null || bucketName.getText().isEmpty() ||
                accessKey == null || accessKey.getText().isEmpty() ||
                secretKey == null || secretKey.getText().isEmpty() ||
                region == null || region.getValue() == null
        ) {
            showAlert();
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }

    private void showAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Invalid Credentials");
        alert.setContentText("Please enter valid credentials");
        alert.showAndWait();
    }
}
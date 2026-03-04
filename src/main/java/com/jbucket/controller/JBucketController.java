package com.jbucket.controller;

import com.jbucket.model.BucketObject;
import com.jbucket.model.BucketSession;
import com.jbucket.model.service.BucketService;
import com.jbucket.view.JBucketApplication;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class JBucketController {
    private BucketService bucketService;

    @FXML
    private MenuItem logoutMenuItem;

    @FXML
    private Button refreshBtn;

    @FXML
    private Button searchBtn;

    @FXML
    private TextField searchBar;

    @FXML
    private Button uploadBtn;

    @FXML
    private Text regionLabel;

    @FXML
    private Text bucketNameLabel;

    @FXML
    private TableView<BucketObject> resultTableView;

    @FXML
    private TableColumn<BucketObject, String> nameColumn;

    @FXML
    private TableColumn<BucketObject, Long> sizeColumn;

    @FXML
    private TableColumn<BucketObject, Instant> lastModifiedColumn;

    @FXML
    private MenuItem downloadMenuItem;

    @FXML
    private MenuItem deleteMenuItem;

    private final ObservableList<BucketObject> tableItems = FXCollections.observableArrayList();


    public JBucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @FXML
    public void initialize() {
        configureExitConfirmation();
        configureBucketLabels();
        configureTable();

        resultTableView.setItems(tableItems);
        loadObjects();
    }

    private void configureExitConfirmation(){
        Platform.runLater(()->{
            Stage stage = (Stage) resultTableView.getScene().getWindow();

            stage.setOnCloseRequest(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Do you really want to exit?");
                alert.initOwner(stage);

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK)
                    closeApp(stage);
                else event.consume();
            });
        });
    }

    private void closeApp(Stage stage){
        bucketService.close();
        stage.close();
    }

    private void connectionTest(){
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                bucketService.connectionCheck();
                return null;
            }
        };

        task.setOnFailed(event -> {
            alertError(event.getSource().getMessage());
        });

        new Thread(task).start();
    }

    private void configureBucketLabels(){
        BucketSession session = bucketService.getSession();
        bucketNameLabel.setText(session.bucketName());
        regionLabel.setText(session.region().toString());
    }

    private void configureTable(){
        configureNameColumn();
        configureSizeColumn();
        configureLastModifiedColumn();
    }

    private void configureNameColumn(){
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    private void configureSizeColumn(){
        sizeColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getSize())
        );

        sizeColumn.setCellFactory(col -> new TableCell<BucketObject, Long>() {
            @Override
            protected void updateItem(Long bytes, boolean empty) {
                super.updateItem(bytes, empty);
                setText(empty ? null : bucketService.getFormatedFileSize(bytes));
            }
        });
    }

    private void configureLastModifiedColumn(){
        lastModifiedColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getLastModified())
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());

        lastModifiedColumn.setCellFactory(col -> new TableCell<BucketObject, Instant>(){
            @Override
            protected void updateItem(Instant data, boolean empty) {
                setText(empty ? null : formatter.format(data));
            }
        });
    }


    @FXML
    public void loadObjects(){
        tableItems.clear();
        Task<List<BucketObject>> task = new Task<>() {
            @Override
            protected List<BucketObject> call() throws Exception {
                return bucketService.listFiles();
            }
        };

        task.setOnSucceeded(event -> {
            tableItems.addAll(task.getValue());
        });

        task.setOnFailed(event ->{
            alertError("Connection error");
        });

        new Thread(task).start();
    }

    private void alertError(String msg){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(msg);
        alert.showAndWait();
    }

    private void alertSuccess(String msg){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(msg);
        alert.showAndWait();
    }

    @FXML
    public void search(){
        tableItems.clear();

        String query = getSearchQuery();
        Task<List<BucketObject>> task = new Task<>() {
            @Override
            protected List<BucketObject> call() throws Exception {
                return bucketService.searchFile(query);
            }
        };

        task.setOnSucceeded(event -> {
            tableItems.addAll(task.getValue());
//            task.getValue().forEach(bucketObject -> {
//                System.out.println("name: "+bucketObject.getName());
//            });
        });

        task.setOnFailed(event -> {
            alertError("Connection error");
        });

        new Thread(task).start();
    }

    private String getSearchQuery(){
        String searchQuery = searchBar.getText();
        if (searchQuery == null || searchQuery.isBlank()) {
            alertError("Search is empty");
        }

        return searchQuery;
    }

    @FXML
    public void upload(){
        Stage stage = (Stage) uploadBtn.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select object");
        File selectedFile = chooser.showOpenDialog(stage);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                bucketService.uploadFile(selectedFile, selectedFile.getName());
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            alertSuccess("File uploaded successfully!");
            loadObjects();
        });

        task.setOnFailed(event -> {
            alertError("Can't upload file");
        });

        new Thread(task).start();
    }

    @FXML
    public void logout(ActionEvent event) throws IOException {
        bucketService.close();

        Stage currentStage = (Stage) resultTableView.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(JBucketApplication.class.getResource("login.fxml"));
        Scene scene = new Scene(loader.load());

        currentStage.hide();
        currentStage.setScene(scene);
        currentStage.setTitle("J-Bucket Login");
        currentStage.setWidth(400);
        currentStage.setHeight(600);
        currentStage.requestFocus();
        currentStage.toFront();
        currentStage.centerOnScreen();
        currentStage.show();

    }

    @FXML
    public void download(ActionEvent event) throws IOException {
        var selectedLine = resultTableView.getSelectionModel();
        String fileName = selectedLine.getSelectedItem().getName();

        Path path;
        try {
            path = getDownloadPath();
        } catch (Exception e){
            return;
        }

        Path destinationPath = Paths.get(path.toString(), fileName);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                bucketService.downloadFile(fileName, destinationPath);
                return null;
            }
        };

        task.setOnFailed(e -> {
            alertError(e.getSource().getException().getMessage());
        });

        task.setOnSucceeded(e -> {
            loadObjects();
            alertSuccess("File Downloaded successfully!");
        });

        if (Files.exists(destinationPath)){
            if(!askToOverwriteFile())
                return;
            else
                Files.delete(destinationPath);
        }
        new Thread(task).start();
    }

    private Path getDownloadPath() throws Exception {
        Stage stage = (Stage) resultTableView.getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a directory");
        File directory = directoryChooser.showDialog(stage);

        if (directory != null) {

            return directory.toPath().toAbsolutePath();
        }
        else {
            throw new Exception("No file selected");
        }
    }

    private boolean askToOverwriteFile(){
        Stage stage = (Stage) resultTableView.getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setHeaderText("This file is already exists");
        alert.setContentText("Are you sure you want to overwrite this file?");
        alert.initOwner(stage);
        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
    }

    @FXML
    public void delete(ActionEvent event) throws IOException {
        var selectedLine = resultTableView.getSelectionModel();
        String fileName = selectedLine.getSelectedItem().getName();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                bucketService.deleteFile(fileName);
                return null;
            }
        };

        task.setOnFailed(e -> {
            alertError("Can't delete file");
        });

        task.setOnSucceeded(e -> {
            loadObjects();
        });

        new Thread(task).start();
    }
}

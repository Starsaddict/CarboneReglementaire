package com.example.zhuantan_calculator.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ThresholdTargetController {
    @FXML
    private void goBack(ActionEvent event) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
        Parent root = loader.load();
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        scene.setRoot(root);
    }
}

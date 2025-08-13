package com.example.zhuantan_calculator.controller;

import com.example.zhuantan_calculator.model.CarbonFactor;
import com.example.zhuantan_calculator.model.CommercialTarget;
import com.example.zhuantan_calculator.model.EnergyConversion;
import com.example.zhuantan_calculator.model.NewEnergyThreshold;
import com.example.zhuantan_calculator.service.CarbonFactorService;
import com.example.zhuantan_calculator.service.CommercialTargetService;
import com.example.zhuantan_calculator.service.EnergyConversionService;
import com.example.zhuantan_calculator.service.NewEnergyThresoldService;
import jakarta.persistence.Persistence;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class ThresholdController {
    @FXML private ChoiceBox<String> choiceBox;
    @FXML private TableView<CommercialTarget> target;
    @FXML private TableView<CarbonFactor> factor ;
    @FXML private TableView<EnergyConversion> conversion ;
    @FXML private TableView<NewEnergyThreshold> threshold;

    private CarbonFactorService carbonFactorService;
    private CommercialTargetService commercialTargetService;
    private EnergyConversionService energyConversionService;
    private NewEnergyThresoldService newEnergyThresoldService;

    private EntityManagerFactory emf;
    private EntityManager em;

    public void initialize() {
        choiceBox.getItems().addAll("CommercialTarget", "CarbonFactor", "EnergyConversion", "NewEnergyThreshold");
        choiceBox.setValue("CommercialTarget");
        showTable("CommercialTarget");

        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showTable(newVal);
        });

        assert choiceBox != null && target != null && factor != null && conversion != null && threshold != null : "FXML injection failed";

        // 初始化 JPA（替换为你 persistence.xml 里的实际 persistence-unit 名称）
        emf = Persistence.createEntityManagerFactory("zhuantanPU");
        em = emf.createEntityManager();

        // 初始化服务
        carbonFactorService = new CarbonFactorService(em);
        commercialTargetService = new CommercialTargetService(em);
        energyConversionService = new EnergyConversionService(em);
        newEnergyThresoldService = new NewEnergyThresoldService(em);

        // 加载四个表格的数据
        target.setItems(FXCollections.observableArrayList(commercialTargetService.getAllCommercialTarget()));
        factor.setItems(FXCollections.observableArrayList(carbonFactorService.getAllCarbonFactor()));
        conversion.setItems(FXCollections.observableArrayList(energyConversionService.getAllEnergyConversion()));
        threshold.setItems(FXCollections.observableArrayList(newEnergyThresoldService.getAllEnergyThreshold()));
    }

    private void showTable(String name) {
        boolean showTarget     = "CommercialTarget".equals(name);
        boolean showFactor     = "CarbonFactor".equals(name);
        boolean showConversion = "EnergyConversion".equals(name);
        boolean showThreshold  = "NewEnergyThreshold".equals(name);

        target.setVisible(showTarget);       target.setManaged(showTarget);
        factor.setVisible(showFactor);       factor.setManaged(showFactor);
        conversion.setVisible(showConversion); conversion.setManaged(showConversion);
        threshold.setVisible(showThreshold);  threshold.setManaged(showThreshold);
    }

    @FXML
    private void goBack(ActionEvent event) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
        Parent root = loader.load();
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        scene.setRoot(root);
    }


}

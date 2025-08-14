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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.scene.control.cell.TextFieldTableCell;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

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

        // 自动按模型生成列，避免在 FXML 里写死
        autoColumns(target, CommercialTarget.class);
        autoColumns(factor, CarbonFactor.class);
        autoColumns(conversion, EnergyConversion.class);
        autoColumns(threshold, NewEnergyThreshold.class);
    }

    private void showTable(String name) {
        boolean showTarget     = "CommercialTarget".equals(name);
        boolean showFactor     = "CarbonFactor".equals(name);
        boolean showConversion = "EnergyConversion".equals(name);
        boolean showThreshold  = "NewEnergyThreshold".equals(name);

        target.setVisible(showTarget);
        target.setManaged(showTarget);
        factor.setVisible(showFactor);
        factor.setManaged(showFactor);
        conversion.setVisible(showConversion);
        conversion.setManaged(showConversion);
        threshold.setVisible(showThreshold);
        threshold.setManaged(showThreshold);
    }

    @FXML
    private void goBack(ActionEvent event) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
        Parent root = loader.load();
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        scene.setRoot(root);
    }

    private <T> void autoColumns(TableView<T> table, Class<T> modelClass) {
        table.setEditable(true);
        table.getColumns().clear();
        for (Field f : modelClass.getDeclaredFields()) {
            String fieldName = f.getName();
            if ("id".equalsIgnoreCase(fieldName)) continue; // 不显示/不编辑 id

            TableColumn<T, Object> col = new TableColumn<>(fieldName);
            col.setPrefWidth(140);
            col.setCellValueFactory(new PropertyValueFactory<>(fieldName));

            Class<?> type = f.getType();
            // 根据字段类型选择可编辑单元格
            if (type == String.class) {
                @SuppressWarnings("unchecked")
                TableColumn<T, String> stringCol = (TableColumn<T, String>) (TableColumn<?, ?>) col;
                stringCol.setCellFactory(TextFieldTableCell.forTableColumn());
                stringCol.setOnEditCommit(ev -> {
                    T row = ev.getRowValue();
                    String newVal = ev.getNewValue();
                    setPropertyViaSetter(row, fieldName, newVal);
                    persistRow(row);
                    table.refresh();
                });
            } else if (type == Integer.class || type == int.class) {
                @SuppressWarnings("unchecked")
                TableColumn<T, Integer> intCol = (TableColumn<T, Integer>) (TableColumn<?, ?>) col;
                intCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
                intCol.setOnEditCommit(ev -> {
                    T row = ev.getRowValue();
                    Integer newVal = ev.getNewValue();
                    setPropertyViaSetter(row, fieldName, newVal);
                    persistRow(row);
                    table.refresh();
                });
            } else if (type == Double.class || type == double.class) {
                @SuppressWarnings("unchecked")
                TableColumn<T, Double> dblCol = (TableColumn<T, Double>) (TableColumn<?, ?>) col;
                dblCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
                dblCol.setOnEditCommit(ev -> {
                    T row = ev.getRowValue();
                    Double newVal = ev.getNewValue();
                    setPropertyViaSetter(row, fieldName, newVal);
                    persistRow(row);
                    table.refresh();
                });
            } else if (type == Boolean.class || type == boolean.class) {
                // 布尔类型先保持只读；如需编辑可改为 CheckBoxTableCell，并在commit时persist
                col.setEditable(false);
            } else {
                // 其他类型默认只读
                col.setEditable(false);
            }

            table.getColumns().add(col);
        }
    }

    private <T> void persistRow(T row) {
        try {
            if (em != null) {
                em.getTransaction().begin();
                em.merge(row);
                em.getTransaction().commit();
            }
        } catch (Exception ex) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            // 这里不抛出，让UI不中断；你也可以改为弹窗提示
            ex.printStackTrace();
        }
    }

    private static void setPropertyViaSetter(Object target, String fieldName, Object value) {
        String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method m = null;
        try {
            // 先按参数实际类型精确匹配
            if (value != null) {
                try {
                    m = target.getClass().getMethod(setter, value.getClass());
                } catch (NoSuchMethodException ignore) {
                    // 尝试用装箱/拆箱后的常见类型匹配
                    Class<?> alt = value instanceof Integer ? int.class : value instanceof Double ? double.class : value instanceof Boolean ? boolean.class : value.getClass();
                    try {
                        m = target.getClass().getMethod(setter, alt);
                    } catch (NoSuchMethodException ignored) { }
                }
            }
            if (m == null) {
                // 回退：按字段声明类型找setter
                Field f = target.getClass().getDeclaredField(fieldName);
                m = target.getClass().getMethod(setter, f.getType());
            }
            m.invoke(target, value);
        } catch (Exception e) {
            // 最后回退：直设字段（需要可访问）
            try {
                Field f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    public void exportThreshold(ActionEvent actionEvent) {
    }
}

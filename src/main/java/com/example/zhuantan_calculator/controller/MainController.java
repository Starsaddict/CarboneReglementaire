package com.example.zhuantan_calculator.controller;

import com.example.zhuantan_calculator.functionalInterface.*;
import com.example.zhuantan_calculator.model.CommercialTarget;
import com.example.zhuantan_calculator.model.HeavyVehicle;
import com.example.zhuantan_calculator.model.LightVehicle;
import com.example.zhuantan_calculator.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.scene.control.ComboBox;
import javafx.stage.Window;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.application.Platform;

import com.example.zhuantan_calculator.model.Vehicles;
import com.example.zhuantan_calculator.factory.VehicleFactory;
import com.example.zhuantan_calculator.util.ExcelReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.ReadOnlyObjectWrapper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import java.io.FileOutputStream;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class MainController {

    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private TableView<Vehicles> vehicleTable;
    @FXML private TableColumn<Vehicles, Integer> yearCol ;
    @FXML private TableColumn<Vehicles, String> enterpriseCol;
    @FXML private TableColumn<Vehicles, String> modelCol;
    @FXML private TableColumn<Vehicles, String> fuelTypeCol;
    @FXML private TableColumn<Vehicles, Integer> curbWeightCol;
    @FXML private TableColumn<Vehicles, Integer> grossWeightCol;
    @FXML private TableColumn<Vehicles, Double> testMassCol;
    @FXML private TableColumn<Vehicles, String> gvwAreaCol;
    @FXML private TableColumn<Vehicles, Double> energyCol;
    @FXML private TableColumn<Vehicles, Integer> salesCol;
    @FXML private TableColumn<Vehicles, String> carbonGroupCol;

    @FXML private TableColumn<Vehicles, String> energyConsumptionMethod0Col;
    @FXML private TableColumn<Vehicles, String> energyConsumptionMethod1Col;
    @FXML private TableColumn<Vehicles, String> energyConsumptionMethod3Col;

    @FXML private TableColumn<Vehicles, String> target0Col;
    @FXML private TableColumn<Vehicles, String> target1Col;
    @FXML private TableColumn<Vehicles, String> target3Col;

    @FXML private TableColumn<Vehicles, String> bonusCol;

    @FXML private TableColumn<Vehicles, Double> netOilCreditMethod0Col;
    @FXML private TableColumn<Vehicles, Double> netOilCreditMethod1Col;
    @FXML private TableColumn<Vehicles, Double> netOilCreditMethod3Col;
    @FXML private TableColumn<Vehicles, Double> netCarbonCreditMethod0Col;
    @FXML private TableColumn<Vehicles, Double> netCarbonCreditMethod1Col;
    @FXML private TableColumn<Vehicles, Double> netCarbonCreditMethod3Col;
    private final ObservableList<Vehicles> vehicles = FXCollections.observableArrayList();
    private final List<Vehicles> vehiclesList = new ArrayList<>();
    // Map to track warning message for each row (vehicle)
    private final java.util.Map<Vehicles, String> rowWarningMap = new java.util.HashMap<>();
    // Anchor map for GVW area cells to show PopOver at the exact cell
    private final java.util.Map<Vehicles, TableCell<Vehicles, String>> gvwAreaCellMap = new java.util.HashMap<>();
    // Guard to prevent double commit when both onAction and focus listeners fire
    private boolean gvwCommitInProgress = false;
    // Map to track per-row Tooltip for warnings (multiple simultaneous tooltips)
    private final java.util.Map<Vehicles, Tooltip> rowTooltipMap = new java.util.HashMap<>();

    private EntityManagerFactory emf;
    private EntityManager em;
    private CarbonFactorService carbonFactorService;
    private CommercialTargetService commercialTargetService;
    private EnergyConversionService energyConversionService;
    private NewEnergyThresoldService newEnergyThresoldService;
    private VehicleService vehicleService;

    private CarbonFactorProvider carbonFactorProvider;
    private ConvertionProvider convertionProvider;
    private NewEnergyThresholdProvider newEnergyThresholdProvider;
    private TargetProvider targetProvider;
    private BonusProvider bonusProvider;

    @FXML void initialize() {
        vehicleTable.setItems(vehicles);
        // Set row factory to style entire row and show tooltip for warnings
        vehicleTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Vehicles item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                    setTooltip(null);
                } else {
                    String msg = rowWarningMap.get(item);
                    if (msg != null && !msg.isEmpty()) {
                        // 整行淡红色背景
                        setStyle("-fx-background-color: rgba(255,0,0,0.12);");
                        // 悬浮提示小对话框
                        setTooltip(new Tooltip(msg));
                    } else {
                        setStyle("");
                        setTooltip(null);
                    }
                }
            }
        });

        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));
        enterpriseCol.setCellValueFactory(new PropertyValueFactory<>("enterprise"));
        fuelTypeCol.setCellValueFactory(new PropertyValueFactory<>("fuelType"));
        curbWeightCol.setCellValueFactory(
                cellData ->{
                    Vehicles v =  cellData.getValue();
                    if( v instanceof LightVehicle){
                        return new ReadOnlyObjectWrapper<>(((LightVehicle) v).getCurbWeight());
                    }
                    return null;
                }
        );
        grossWeightCol.setCellValueFactory(new PropertyValueFactory<>("grossWeight"));
        testMassCol.setCellValueFactory(
                cellData -> {
                    Vehicles v =  cellData.getValue();
                    if( v instanceof LightVehicle){
                        return new ReadOnlyObjectWrapper<>(((LightVehicle) v).getTestMass());
                    }
                    return null;
                }
        );
        gvwAreaCol.setCellValueFactory(
                cellData -> {
                    Vehicles v =  cellData.getValue();
                    if(v instanceof HeavyVehicle){
                        return new ReadOnlyObjectWrapper<>(((HeavyVehicle) v).getGvwArea());
                    }
                    return null;
                }
        );
        energyCol.setCellValueFactory(new PropertyValueFactory<>("energy"));
        salesCol.setCellValueFactory(new PropertyValueFactory<>("sales"));
        carbonGroupCol.setCellValueFactory(new PropertyValueFactory<>("carbonGroup"));


        useDecimalDisplay(energyCol, "#,##0.00");

        vehicleTable.setEditable(true);
        gvwAreaCol.setEditable(true);
        setupGvwAreaComboCell();

        // Initialize services and providers
        emf = Persistence.createEntityManagerFactory("zhuantanPU");
        em = emf.createEntityManager();
        carbonFactorService = new CarbonFactorService(em);
        commercialTargetService = new CommercialTargetService(em);
        energyConversionService = new EnergyConversionService(em);
        newEnergyThresoldService = new NewEnergyThresoldService(em);
        vehicleService = new VehicleService();

        carbonFactorProvider = v -> carbonFactorService.getCarbonFactor(v.computeCarbonFuelType());
        convertionProvider = (v, method) -> {
            String fuelType = v.getFuelType();
            if ("天然气".equals(fuelType)) {
                if (v.getCarbonGroup().equals("牵引车") || v.getCarbonGroup().equals("中重型载货") || v.getCarbonGroup().equals("自卸车")) {
                    fuelType = "天然气-LNG";
                } else {
                    fuelType = "天然气-CNG";
                }
            }
            System.out.println(v.getModel()+"的燃料类型为" + fuelType);
            Double coeff = energyConversionService.computeConversionCoeff(fuelType, v.computeCarbonFuelType(), method);
            if ("汽油".equals(fuelType) || "柴油".equals(fuelType)) {
                coeff = 1.0;
            }
            System.out.println(v.getModel()+"的燃料转换系数为" + coeff);
            return coeff;
        };
        newEnergyThresholdProvider = v -> {
            Double threshold = newEnergyThresoldService.computeNewEnergyThreshold(v.getYear(), v.getCarbonGroup());
            double penetrationRate = vehicleService.computePenetrationRate(vehiclesList, v);
            System.out.println("threshold: " + threshold + ", penetrationRate: " + penetrationRate);
            return threshold;
        };
        targetProvider = (HeavyVehicle v, int method) -> commercialTargetService.getTarget(v.getYear(), v.getCarbonModel(), v.getFuelType(), v.getGrossWeight(), v.getGvwArea(), method);
        bonusProvider = v -> {
            double threshold = newEnergyThresholdProvider.getEnergyThreshold(v);
            double penetrationRate = vehicleService.computePenetrationRate(vehiclesList, v);
            System.out.println(v.getModel() + " 新能源渗透率：" + penetrationRate);
            return penetrationRate >= threshold ? 1.03 : 1;
        };

        exportButton.setOnAction(this::handleExportExcel);

        energyConsumptionMethod0Col.setVisible(false);
        energyConsumptionMethod1Col.setVisible(false);
        energyConsumptionMethod3Col.setVisible(false);
        target0Col.setVisible(false);
        target1Col.setVisible(false);
        target3Col.setVisible(false);
        bonusCol.setVisible(false);
        netOilCreditMethod0Col.setVisible(false);
        netOilCreditMethod1Col.setVisible(false);
        netOilCreditMethod3Col.setVisible(false);
        netCarbonCreditMethod0Col.setVisible(false);
        netCarbonCreditMethod1Col.setVisible(false);
        netCarbonCreditMethod3Col.setVisible(false);

    }

    @FXML
    private void handleImportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        Window ownerWindow = importButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile != null) {
            ExcelReader reader = new ExcelReader();
            List<Map<String, String>> rawRows = reader.read(selectedFile);

            int rowIndex = 2;
            for (Map<String, String> row : rawRows) {
                try {
                    Vehicles v = VehicleFactory.createVehicleFromData(
                            parseIntSafe(row.get("年份")),
                            row.get("车辆生产企业"),
                            row.get("车辆型号"),
                            parseIntSafe(row.get("整备质量/kg")),
                            parseIntSafe(row.get("总质量/kg")),
                            parseDoubleSafe(row.get("测试质量/kg")),
                            row.get("质量段"),
                            parseDoubleSafe(row.get("能耗")),
                            row.get("燃料种类"),
                            row.get("转碳车组"),
                            parseIntSafe(row.get("销量"))
                    );
                    vehiclesList.add(v);
                } catch (Exception e) {
                    System.err.println("行解析失败 (第 " + rowIndex + " 行): " + row + ", 错误: " + e.getMessage());
                }
                rowIndex++;
            }

            vehicles.setAll(vehiclesList);
        }
    }
    private Integer parseIntSafe(String value) {
        try {
            return (int)Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void handleExportExcel(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        Window ownerWindow = importButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            exportToExcel(file);
        }
    }

    private void exportToExcel(File file) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("数据");
            // Header
            Row headerRow = sheet.createRow(0);
            var columns = vehicleTable.getColumns();
            for (int col = 0; col < columns.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns.get(col).getText());
            }
            // Data rows
            var items = vehicleTable.getItems();
            for (int rowIndex = 0; rowIndex < items.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                var v = items.get(rowIndex);
                for (int col = 0; col < columns.size(); col++) {
                    @SuppressWarnings("unchecked")
                    TableColumn<Vehicles, Object> tc = (TableColumn<Vehicles, Object>) columns.get(col);
                    Object value = tc.getCellData(v);
                    Cell cell = row.createCell(col);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }
            // Auto-size
            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            System.out.println("导出成功：" + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void calculateCredit() {
        // 计算净油耗
        Map<Vehicles, Double> consumption0Map = new HashMap<>();
        Map<Vehicles, Double> consumption1Map = new HashMap<>();
        Map<Vehicles, Double> consumption3Map = new HashMap<>();

        // 计算目标值
        Map<Vehicles, Double> target0Map = new HashMap<>();
        Map<Vehicles, Double> target1Map = new HashMap<>();
        Map<Vehicles, Double> target3Map = new HashMap<>();

        //计算新能源调节系数
        Map<Vehicles, Double> bonusMap = new HashMap<>();

        // 计算净油积分（方法0/1/3）
        Map<Vehicles, Double> credit0Map = new HashMap<>();
        Map<Vehicles, Double> credit1Map = new HashMap<>();
        Map<Vehicles, Double> credit3Map = new HashMap<>();
        // 计算净碳积分
        Map<Vehicles, Double> carbonCredit0Map = new HashMap<>();
        Map<Vehicles, Double> carbonCredit1Map = new HashMap<>();
        Map<Vehicles, Double> carbonCredit3Map = new HashMap<>();
        for (Vehicles v : vehiclesList) {
            double consumption0 = v.computeOilConsumption(convertionProvider,0);
            double consumption1 = v.computeOilConsumption(convertionProvider,1);
            double consumption3 = v.computeOilConsumption(convertionProvider,3);

            double target0 = v.computeTarget(targetProvider,0);
            double target1 = v.computeTarget(targetProvider,1);
            double target3 = v.computeTarget(targetProvider,3);

            double bonus = bonusProvider.calculateBonus(v);

            double credit0 = v.computeNetOilCredit(bonusProvider, targetProvider, convertionProvider, 0);
            double credit1 = v.computeNetOilCredit(bonusProvider, targetProvider, convertionProvider, 1);
            double credit3 = v.computeNetOilCredit(bonusProvider, targetProvider, convertionProvider, 3);
            credit1Map.put(v, credit1);
            credit3Map.put(v, credit3);

            // 计算净碳积分
            double carbonCredit0 = v.computeNetCarbonCredit(carbonFactorProvider, credit0);
            double carbonCredit1 = v.computeNetCarbonCredit(carbonFactorProvider, credit1);
            double carbonCredit3 = v.computeNetCarbonCredit(carbonFactorProvider, credit3);
            carbonCredit0Map.put(v, carbonCredit0);
            carbonCredit1Map.put(v, carbonCredit1);
            carbonCredit3Map.put(v, carbonCredit3);

            consumption0Map.put(v, consumption0);
            consumption1Map.put(v, consumption1);
            consumption3Map.put(v, consumption3);

            target0Map.put(v, target0);
            target1Map.put(v, target1);
            target3Map.put(v, target3);

            bonusMap.put(v, bonus);

            credit0Map.put(v, credit0);
        }
        // 取数
        energyConsumptionMethod0Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(consumption0Map.get(cellData.getValue())).asString()
        );
        energyConsumptionMethod1Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(consumption1Map.get(cellData.getValue())).asString());
        energyConsumptionMethod3Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(consumption3Map.get(cellData.getValue())).asString());

        target0Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(target0Map.get(cellData.getValue())).asString());
        target1Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(target1Map.get(cellData.getValue())).asString());
        target3Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(target3Map.get(cellData.getValue())).asString());

        bonusCol.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(bonusMap.get(cellData.getValue())).asString());

        netOilCreditMethod0Col.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(credit0Map.get(cellData.getValue()))
        );
        netOilCreditMethod1Col.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(credit1Map.get(cellData.getValue()))
        );
        netOilCreditMethod3Col.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(credit3Map.get(cellData.getValue()))
        );
        netCarbonCreditMethod0Col.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(carbonCredit0Map.get(cellData.getValue()))
        );
        netCarbonCreditMethod1Col.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(carbonCredit1Map.get(cellData.getValue()))
        );
        netCarbonCreditMethod3Col.setCellValueFactory(cellData ->
            new ReadOnlyObjectWrapper<>(carbonCredit3Map.get(cellData.getValue()))
        );
        // 3) 刷新表格显示

        vehicleTable.refresh();

        // 隐藏基础列
        fuelTypeCol.setVisible(false);
        curbWeightCol.setVisible(false);
        grossWeightCol.setVisible(false);
        testMassCol.setVisible(false);
        gvwAreaCol.setVisible(false);
        energyCol.setVisible(false);
        salesCol.setVisible(false);
        carbonGroupCol.setVisible(false);

        energyConsumptionMethod0Col.setVisible(true);
        energyConsumptionMethod1Col.setVisible(true);
        energyConsumptionMethod3Col.setVisible(true);
        target0Col.setVisible(true);
        target1Col.setVisible(true);
        target3Col.setVisible(true);
        bonusCol.setVisible(true);
        netOilCreditMethod0Col.setVisible(true);
        netOilCreditMethod1Col.setVisible(true);
        netOilCreditMethod3Col.setVisible(true);
        netCarbonCreditMethod0Col.setVisible(true);
        netCarbonCreditMethod1Col.setVisible(true);
        netCarbonCreditMethod3Col.setVisible(true);

    }

    private Double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshTable() {
        vehicleTable.getItems().clear();
        vehicleTable.getItems().addAll(vehicles);
    }

    private static <S> void useDecimalDisplay(TableColumn<S, Double> col, String pattern) {
        // 只创建一次，避免在每个单元格更新时反复 new
        DecimalFormat df = new DecimalFormat(pattern);
        col.setCellFactory(tc -> new TableCell<S, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(df.format(value));
                }
            }
        });
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
    }

    private void setupGvwAreaComboCell() {
        gvwAreaCol.setCellFactory(col -> new TableCell<Vehicles, String>() {
            private ComboBox<String> combo;
            private void createCombo(String carbonGroup, String currentValue) {
                combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                try {
                    var options = commercialTargetService.listGvwAreasByCarbonGroup(carbonGroup);
                    combo.getItems().setAll(options);
                } catch (Exception ex) {
                    combo.getItems().clear();
                }
                combo.getSelectionModel().select(currentValue);
                combo.setOnAction(e -> {
                    if (gvwCommitInProgress) return;
                    gvwCommitInProgress = true;
                    try {
                        commitEdit(combo.getSelectionModel().getSelectedItem());
                    } finally {
                        gvwCommitInProgress = false;
                    }
                });
                combo.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) { // focus lost
                        if (gvwCommitInProgress) return;
                        String sel = combo.getSelectionModel().getSelectedItem();
                        if (!java.util.Objects.equals(sel, getItem())) {
                            gvwCommitInProgress = true;
                            try {
                                commitEdit(sel);
                            } finally {
                                gvwCommitInProgress = false;
                            }
                        }
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                Vehicles v = getTableView().getItems().get(getIndex());
                if (v instanceof HeavyVehicle) {
                    String carbonGroup = v.getCarbonGroup();
                    String currentValue = getItem();
                    createCombo(carbonGroup, currentValue);
                    setText(null);
                    setGraphic(combo);
                    combo.requestFocus();
                    combo.show();
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
            }

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    try {
                        Vehicles prev = getTableView().getItems().get(getIndex());
                        gvwAreaCellMap.remove(prev);
                    } catch (Exception ignore) {}
                    return;
                }
                Vehicles v = getTableView().getItems().get(getIndex());
                gvwAreaCellMap.put(v, this);
                if (isEditing()) {
                    if (v instanceof HeavyVehicle) {
                        String carbonGroup = v.getCarbonGroup();
                        createCombo(carbonGroup, value);
                        setText(null);
                        setGraphic(combo);
                        combo.requestFocus();
                        setStyle("");
                    } else {
                        setText(value);
                        setGraphic(null);
                        setStyle("");
                    }
                } else {
                    setText(value);
                    setGraphic(null);
                    setStyle("");
                }
            }
        });
        // Add OnEditCommit handler to update HeavyVehicle and refresh table, show Tooltip for warnings (multiple tooltips supported)
        gvwAreaCol.setOnEditCommit(event -> {
            Vehicles v = event.getRowValue();
            boolean hasWarning = false;
            StringBuilder messageBuilder = new StringBuilder();
            if (v instanceof HeavyVehicle) {
                String newVal = event.getNewValue();
                String carbonGroup = v.getCarbonGroup();

                // 让 service 层决定校验结论：返回 "ok" 代表通过，否则返回具体错误信息
                String returnMessage = commercialTargetService.ifMatchGVMArea(carbonGroup, v.getGrossWeight(), newVal);
                if (!"ok".equalsIgnoreCase(returnMessage)) {
                    hasWarning = true;
                    messageBuilder.append(returnMessage);
                }
                // 先写回模型，再刷新；避免刷新使得旧的 cell 引用失效
                ((HeavyVehicle) v).setGvwArea(newVal);
                if (hasWarning) {
                    rowWarningMap.put(v, messageBuilder.toString());
                } else {
                    rowWarningMap.remove(v);
                }
                vehicleTable.refresh();

                if (hasWarning) {
                    // 在下一帧获取最新的 cell 并在其附近显示 Tooltip（允许多个同时存在）
                    Platform.runLater(() -> {
                        TableCell<Vehicles, String> cell = gvwAreaCellMap.get(v);
                        String msg = messageBuilder.toString();
                        Tooltip old = rowTooltipMap.remove(v);
                        if (old != null) {
                            old.hide();
                        }
                        Tooltip tip = new Tooltip(msg);
                        tip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(255,255,200,0.95); -fx-text-fill: black;");
                        if (cell != null && cell.getScene() != null && cell.isVisible()) {
                            var b = cell.localToScreen(cell.getBoundsInLocal());
                            if (b != null) {
                                double screenX = b.getMaxX() + 8;
                                double screenY = (b.getMinY() + b.getMaxY()) / 2.0; // 垂直居中
                                tip.show(cell, screenX, screenY);
                                rowTooltipMap.put(v, tip);
                                // 自动隐藏但不影响其它行
                                PauseTransition hideLater = new PauseTransition(Duration.seconds(4));
                                hideLater.setOnFinished(e -> {
                                    tip.hide();
                                    rowTooltipMap.remove(v, tip);
                                });
                                hideLater.play();
                                return;
                            }
                        }
                        // 兜底：锚到表格左上角偏移，至少能看到（极少触发）
                        if (vehicleTable != null && vehicleTable.getScene() != null) {
                            var p = vehicleTable.localToScreen(0, 0);
                            double screenX = p.getX() + 20;
                            double screenY = p.getY() + 20;
                            tip.show(vehicleTable, screenX, screenY);
                            rowTooltipMap.put(v, tip);
                            PauseTransition hideLater = new PauseTransition(Duration.seconds(4));
                            hideLater.setOnFinished(e -> {
                                tip.hide();
                                rowTooltipMap.remove(v, tip);
                            });
                            hideLater.play();
                        }
                    });
                } else {
                    // 若本次无警告但之前显示过 Tooltip，则关闭并移除
                    Tooltip old = rowTooltipMap.remove(v);
                    if (old != null) {
                        old.hide();
                    }
                }
            }
        });
    }

    private boolean isGvwAreaMatchingWeight(String gvwArea, Integer grossWeight) {
        if (grossWeight == null || gvwArea == null) {
            return false;
        }
        // Example: If gvwArea strings are like "1000-2000", parse min and max and check range.
        try {
            String[] parts = gvwArea.split("-");
            if (parts.length == 2) {
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                return grossWeight > min && grossWeight <= max;
            }
        } catch (Exception e) {
            // parsing error
        }
        return false;
    }

}

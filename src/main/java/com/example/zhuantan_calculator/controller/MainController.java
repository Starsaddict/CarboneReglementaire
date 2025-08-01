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
import javafx.stage.Window;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
            System.out.println("threshold: " + threshold);
            return threshold;
        };
        targetProvider = (HeavyVehicle v, int method) -> commercialTargetService.getTarget(v.getYear(), v.getCarbonModel(), v.getFuelType(), v.getGrossWeight(), v.getGvwArea(), method);
        bonusProvider = v -> {
            double threshold = newEnergyThresholdProvider.getEnergyThreshold(v);
            double penetrationRate = vehicleService.computePenetrationRate(vehiclesList, v);
            return penetrationRate >= threshold ? 1.03 : 1;
        };

        exportButton.setOnAction(this::handleExportExcel);

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

        // 计算净油积分（方法0）
        Map<Vehicles, Double> credit0Map = new HashMap<>();
        for (Vehicles v : vehiclesList) {
            double consumption0 = v.computeOilConsumption(convertionProvider,0);
            double consumption1 = v.computeOilConsumption(convertionProvider,1);
            double consumption3 = v.computeOilConsumption(convertionProvider,3);

            double target0 = v.computeTarget(targetProvider,0);
            double target1 = v.computeTarget(targetProvider,1);
            double target3 = v.computeTarget(targetProvider,3);

            double bonus = bonusProvider.calculateBonus(v);

            double credit0 = v.computeNetOilCredit(bonusProvider, targetProvider, convertionProvider, 0);

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
        // 3) 刷新表格显示
        vehicleTable.refresh();
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

}

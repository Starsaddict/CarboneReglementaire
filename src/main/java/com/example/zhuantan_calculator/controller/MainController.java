package com.example.zhuantan_calculator.controller;

import com.example.zhuantan_calculator.functionalInterface.*;
import com.example.zhuantan_calculator.model.HeavyVehicle;
import com.example.zhuantan_calculator.model.LightVehicle;
import com.example.zhuantan_calculator.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

import javafx.util.converter.DoubleStringConverter;

import com.example.zhuantan_calculator.model.Vehicles;
import com.example.zhuantan_calculator.factory.VehicleFactory;
import com.example.zhuantan_calculator.util.ExcelReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import javafx.beans.property.ReadOnlyObjectWrapper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import java.io.FileOutputStream;

import java.util.*;

public class MainController {

    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private TableView<Vehicles> vehicleTable;
    @FXML private TableColumn<Vehicles, Integer> yearCol ;
    @FXML private TableColumn<Vehicles, String> enterpriseCol;
    @FXML private TableColumn<Vehicles, String> modelCol;
    @FXML private TableColumn<Vehicles, String> fuelTypeCol;
    @FXML private TableColumn<Vehicles, Double> energyCol;
    @FXML private TableColumn<Vehicles, String> carbonFuelTypeCol;

    @FXML private TableColumn<Vehicles, String> PHEVFuel1Col;
    @FXML private TableColumn<Vehicles, String> PHEVFuel2Col;
    @FXML private TableColumn<Vehicles, Double> PHEVFuel1EnergyCol;
    @FXML private TableColumn<Vehicles, Double> PHEVFuel2EnergyCol;
    @FXML private TableColumn<Vehicles, Integer> curbWeightCol;
    @FXML private TableColumn<Vehicles, Integer> grossWeightCol;
    @FXML private TableColumn<Vehicles, Double> testMassCol;
    @FXML private TableColumn<Vehicles, String> gvwAreaCol;

    @FXML private TableColumn<Vehicles, Integer> salesCol;
    @FXML private TableColumn<Vehicles, String> carbonGroupCol;

    @FXML private TableColumn<Vehicles, Double> energyConsumptionMethod0Col;
    @FXML private TableColumn<Vehicles, Double> energyConsumptionMethod1Col;
    @FXML private TableColumn<Vehicles, Double> energyConsumptionMethod3Col;

    @FXML private TableColumn<Vehicles, Double> target0Col;
    @FXML private TableColumn<Vehicles, Double> target1Col;
    @FXML private TableColumn<Vehicles, Double> target3Col;

    @FXML private TableColumn<Vehicles, Double> penetrationRateCol;

    @FXML private TableColumn<Vehicles, Double> bonusCol;

    @FXML private TableColumn<Vehicles, Double> netOilCreditMethod0Col;
    @FXML private TableColumn<Vehicles, Double> netOilCreditMethod1Col;
    @FXML private TableColumn<Vehicles, Double> netOilCreditMethod3Col;
    @FXML private TableColumn<Vehicles, Double> netCarbonCreditMethod0Col;
    @FXML private TableColumn<Vehicles, Double> netCarbonCreditMethod1Col;
    @FXML private TableColumn<Vehicles, Double> netCarbonCreditMethod3Col;


    private final ObservableList<Vehicles> vehicles = FXCollections.observableArrayList();
    private final List<Vehicles> vehiclesList = new ArrayList<>();
    // Map to track warning message for each row (vehicle)
    private final java.util.Map<Vehicles, String> rowWarningMap = new HashMap<>();
    // Anchor map for GVW area cells to show PopOver at the exact cell
    private final java.util.Map<Vehicles, TableCell<Vehicles, String>> gvwAreaCellMap = new HashMap<>();
    // Anchor map for LightVehicle testMass cells to show Tooltip at the testMass cell
    private final Map<Vehicles, TableCell<Vehicles, Double>> testMassCellMap = new HashMap<>();
    // Guard to prevent double commit when both onAction and focus listeners fire
    private boolean gvwCommitInProgress = false;
    // Map to track per-row Tooltip for warnings (multiple simultaneous tooltips)
    private final Map<Vehicles, Tooltip> rowTooltipMap = new HashMap<>();
    // Map of test mass recommended
    private final Map<Vehicles, Double> TMRecommend = new HashMap<>();

    // 单元格错误高亮样式（淡红，不影响整行）
    private static final String CELL_ERROR_STYLE = "-fx-background-color: rgba(255,0,0,0.18);";

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



    // —— 可自定义的提示文案提供器 ——
    @FunctionalInterface
    interface WarningMessageProvider {
        /**
         * @param v         车辆对象
         * @param defaultMsg 系统生成/校验得到的默认提示
         * @return 供界面展示的最终提示文案
         */
        String get(Vehicles v, String defaultMsg);
    }
    // 默认：直接使用系统生成的提示
    private WarningMessageProvider warningMessageProvider = (v, def) -> def;

    public void setWarningMessageProvider(WarningMessageProvider provider) {
        this.warningMessageProvider = (provider == null) ? (v, d) -> d : provider;
    }

    // —— Null-safe converters：空字符串 ⇒ null —— //
    private static final class NullSafeIntegerStringConverter extends IntegerStringConverter {
        @Override
        public Integer fromString(String value) {
            if (value == null) return null;
            String s = value.trim();
            if (s.isEmpty()) return null;
            return super.fromString(s);
        }
        @Override
        public String toString(Integer value) {
            return value == null ? "" : super.toString(value);
        }
    }
    private static final class NullSafeDoubleStringConverter extends DoubleStringConverter {
        @Override
        public Double fromString(String value) {
            if (value == null) return null;
            String s = value.trim();
            if (s.isEmpty()) return null;
            return super.fromString(s);
        }
        @Override
        public String toString(Double value) {
            return value == null ? "" : super.toString(value);
        }
    }

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
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                } else {
                    // 保持行样式为默认；行提示改为悬浮查看（如果需要，在未来可添加整行标红）
                    setStyle("");
                    // 动态悬浮提示，显示之前缓存的报错信息
                    String latest = rowWarningMap.get(item);
                    if (latest == null || latest.isEmpty()) {
                        setTooltip(null);
                        setOnContextMenuRequested(null);
                        setContextMenu(null);
                    } else {
                        Tooltip tip = new Tooltip();
                        tip.setShowDelay(javafx.util.Duration.millis(260)); // 行级稍慢显示
                        tip.setHideDelay(javafx.util.Duration.millis(150)); // 行级稍快收起
                        tip.setShowDuration(javafx.util.Duration.seconds(8)); // 显示时间更长
                        tip.setOnShowing(e -> {
                            String cur = rowWarningMap.get(item);
                            if (cur == null || cur.isEmpty()) {
                                tip.setText("");
                            } else {
                                tip.setText(warningMessageProvider.get(item, cur));
                            }
                        });
                        tip.setStyle("-fx-background-radius: 8; -fx-background-color: rgba(240,248,255,0.95); -fx-text-fill: black; -fx-padding: 8; -fx-font-size: 12px;");
                        setTooltip(tip);
                        setOnContextMenuRequested(null);
                        setContextMenu(null);
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
        // 让“整备质量/kg”可编辑（轻型车）
        curbWeightCol.setEditable(true);
        curbWeightCol.setCellFactory(col -> new TextFieldTableCell<Vehicles, Integer>(new NullSafeIntegerStringConverter()) {
            @Override
            public void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setStyle("");
                    setTooltip(null);
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                    return;
                }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (v instanceof LightVehicle && rowWarningMap.containsKey(v)) {
                    setStyle(CELL_ERROR_STYLE);
                    attachHoverWarning(this, v);
                    attachRightClickWarning(this, v);
                } else {
                    setStyle("");
                    setTooltip(null);
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                }
            }
        });
        curbWeightCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Integer newVal = evt.getNewValue();
            if (v instanceof LightVehicle) {
                ((LightVehicle) v).setCurbWeight(newVal);
                // 轻型车：改动整备质量后重新校验
                String warn = validateLightVehicle((LightVehicle) v);
                if (warn != null && !warn.isEmpty()) {
                    rowWarningMap.put(v, warn);
                    showGvwWarning(v, warn);
                } else {
                    rowWarningMap.remove(v);
                    hideGvwWarning(v);
                }
                vehicleTable.refresh();
            }
        });
        grossWeightCol.setCellValueFactory(new PropertyValueFactory<>("grossWeight"));
        // 让“总质量/kg”可编辑（所有车辆）
        grossWeightCol.setEditable(true);
        grossWeightCol.setCellFactory(col -> new TextFieldTableCell<Vehicles, Integer>(new NullSafeIntegerStringConverter()) {
            @Override
            public void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setStyle("");
                    setTooltip(null);
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                    return;
                }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (v instanceof LightVehicle && rowWarningMap.containsKey(v)) {
                    setStyle(CELL_ERROR_STYLE);
                    attachHoverWarning(this, v);
                    attachRightClickWarning(this, v);
                } else {
                    setStyle("");
                    setTooltip(null);
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                }
            }
        });
        grossWeightCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Integer newVal = evt.getNewValue();

            // 直接写回：NullSafeIntegerStringConverter 已保证空串 -> null
            v.setGrossWeight(newVal);

            // —— 编辑总质量后，针对不同车型重新校验 —— //
            String warn = null;
            if (v instanceof HeavyVehicle) {
                String gvw = ((HeavyVehicle) v).getGvwArea();

                String msg = commercialTargetService.ifMatchGVMArea(v.getCarbonGroup(), v.getGrossWeight(), gvw);
                if (!"ok".equalsIgnoreCase(msg)) {
                    warn = msg;
                }
            } else if (v instanceof LightVehicle) {
                warn = validateLightVehicle((LightVehicle) v);
            }

            if (warn != null && !warn.isEmpty()) {
                rowWarningMap.put(v, warn);
            } else {
                rowWarningMap.remove(v);
            }

            if (v instanceof LightVehicle) {
                if (warn != null && !warn.isEmpty()) {
                    showGvwWarning(v, warn);
                } else {
                    hideGvwWarning(v);
                }
            }
            vehicleTable.refresh();
        });
        testMassCol.setCellValueFactory(
                cellData -> {
                    Vehicles v =  cellData.getValue();
                    if( v instanceof LightVehicle){
                        return new ReadOnlyObjectWrapper<>(((LightVehicle) v).getTestMass());
                    }
                    return null;
                }
        );
        // 让“测试质量/kg”可编辑（轻型车）
        testMassCol.setEditable(true);
        testMassCol.setCellFactory(col -> {
            TextFieldTableCell<Vehicles, Double> cell = new TextFieldTableCell<>(new NullSafeDoubleStringConverter()) {
                @Override
                public void updateItem(Double value, boolean empty) {
                    super.updateItem(value, empty);
                    if (empty) {
                        setStyle("");
                        setTooltip(null);
                        try {
                            Vehicles prev = getTableView().getItems().get(getIndex());
                            testMassCellMap.remove(prev);
                        } catch (Exception ignore) {}
                        return;
                    }
                    Vehicles v = getTableView().getItems().get(getIndex());
                    // 仅记录轻型车的测试质量单元格作为锚点
                    if (v instanceof LightVehicle) {
                        testMassCellMap.put(v, this);
                    }
                    // 轻型车：该列为错误时标红
                    if (v instanceof LightVehicle && rowWarningMap.containsKey(v)) {
                        setStyle(CELL_ERROR_STYLE);
                        attachHoverWarning(this, v);
                        attachRightClickWarning(this, v);
                    } else {
                        setStyle("");
                        setOnContextMenuRequested(null);
                        setContextMenu(null);
                        setTooltip(null);
                    }
                }
            };
            return cell;
        });
        testMassCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Double newVal = evt.getNewValue();
            if (v instanceof LightVehicle) {
                ((LightVehicle) v).setTestMass(newVal);
                // 轻型车：改动测试质量后重新校验
                String warn = validateLightVehicle((LightVehicle) v);
                if (warn != null && !warn.isEmpty()) {
                    rowWarningMap.put(v, warn);
                } else {
                    rowWarningMap.remove(v);
                }
                if (warn != null && !warn.isEmpty()) {
                    showGvwWarning(v, warn);
                } else {
                    hideGvwWarning(v);
                }
                vehicleTable.refresh();
            }
        });
        gvwAreaCol.setCellValueFactory(
                cellData -> {
                    Vehicles v =  cellData.getValue();
                    if(v instanceof HeavyVehicle){
                        return new ReadOnlyObjectWrapper<>(((HeavyVehicle) v).getGvwArea());
                    }
                    return null;
                }
        );

        // 让“年份”可编辑（所有车辆）
        yearCol.setEditable(true);
        setupYearComboCell();

        energyCol.setCellValueFactory(new PropertyValueFactory<>("energy"));
        PHEVFuel1Col.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel1()));
        PHEVFuel2Col.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel2()));

        PHEVFuel1EnergyCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel1Energy()));
        PHEVFuel2EnergyCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel2Energy()));

        salesCol.setCellValueFactory(new PropertyValueFactory<>("sales"));
        carbonGroupCol.setCellValueFactory(new PropertyValueFactory<>("carbonGroup"));


        useDecimalDisplay(energyCol, "#,##0.00");

        vehicleTable.setEditable(true);
        gvwAreaCol.setEditable(true);
        // 调整“质量段”列的默认宽度，避免编辑区域过窄
        gvwAreaCol.setPrefWidth(120);
        gvwAreaCol.setMinWidth(120);
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
            if (!"PHEV".equals(fuelType)) {
                if ("天然气".equals(fuelType)) {
                    if (v.getCarbonGroup().equals("牵引车") || v.getCarbonGroup().equals("中重型载货") || v.getCarbonGroup().equals("自卸车")) {
                        fuelType = "天然气-LNG";
                    } else {
                        fuelType = "天然气-CNG";
                    }
                }

                Double coeff = energyConversionService.computeConversionCoeff(fuelType, v.computeCarbonFuelType(), method);
                if ("汽油".equals(fuelType) || "柴油".equals(fuelType)) {
                    coeff = 1.0;
                }
                return coeff * v.getEnergy();
            }else{
                Map<String, Double> fuelTypeEnergyMap =  v.getFuelTypeEnergyMap();
                double consumption = 0.0;
                for (Map.Entry<String, Double> entry : fuelTypeEnergyMap.entrySet()) {
                    String subFuelType = entry.getKey();   // 子燃料类型，比如“汽油”“电”
                    Double subEnergy = entry.getValue();  // 子燃料能耗
                    double coeff = energyConversionService.computeConversionCoeff(subFuelType,v.computeCarbonFuelType(), method);
                    double subConsumption = coeff * subEnergy;
                    consumption += subConsumption;
                }
                return consumption;
            }

        };

        newEnergyThresholdProvider = v -> {
            Double threshold = newEnergyThresoldService.computeNewEnergyThreshold(v.getYear(), v.getCarbonGroup());
            double penetrationRate = vehicleService.computePenetrationRate(vehiclesList, v);

            return threshold;
        };
        targetProvider = (HeavyVehicle v, int method) -> commercialTargetService.getTarget(v.getYear(), v.getCarbonModel(), v.getFuelType(), v.getGrossWeight(), v.getGvwArea(), method);
        bonusProvider = v -> {
            double threshold = newEnergyThresholdProvider.getEnergyThreshold(v);
            double penetrationRate = vehicleService.computePenetrationRate(vehiclesList, v);

            return penetrationRate >= threshold ? 1.03 : 1;
        };

        exportButton.setOnAction(this::handleExportExcel);

        showBaseView();

    }

    @FXML
    private void handleImportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        Window ownerWindow = importButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(ownerWindow);
        if (selectedFile != null) {
            resetStateForNewImport();
            ExcelReader reader = new ExcelReader();
            List<Map<String, String>> rawRows = reader.read(selectedFile);

            int rowIndex = 2;
            for (Map<String, String> row : rawRows) {
                try {
                    Vehicles v = VehicleFactory.createVehicleFromData(
                            parseIntSafe(emptyToNull(row.get("年份"))),
                            emptyToNull(row.get("车辆生产企业")),
                            emptyToNull(row.get("车辆型号")),
                            parseIntSafe(emptyToNull(row.get("整备质量/kg"))),
                            parseIntSafe(emptyToNull(row.get("总质量/kg"))),
                            parseDoubleSafe(emptyToNull(row.get("测试质量/kg"))),
                            emptyToNull(row.get("质量段")),
                            parseDoubleSafe(emptyToNull(row.get("能耗"))),
                            emptyToNull(row.get("燃料种类")),
                            emptyToNull(row.get("转碳车组")),
                            parseIntSafe(emptyToNull(row.get("销量"))),
                            emptyToNull(row.get("PHEV燃料1")),
                            parseDoubleSafe(emptyToNull(row.get("PHEV燃料1能耗"))),
                            emptyToNull(row.get("PHEV燃料2")),
                            parseDoubleSafe(emptyToNull(row.get("PHEV燃料2能耗")))
                    );
                    vehiclesList.add(v);
                } catch (Exception e) {
                    System.err.println("行解析失败 (第 " + rowIndex + " 行): " + row + ", 错误: " + e.getMessage());
                }
                rowIndex++;
            }

            vehicles.setAll(vehiclesList);
            showBaseView();
            validateGvwAfterImport();
            validateLightAfterImport();
        }
    }
    private Integer parseIntSafe(String value) {
        if(value == null || value.equals("")) {
            return null;
        }
        try {
            return (int)Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将空字符串（或全空白）转为 null，非空返回去除首尾空格的字符串。
     */
    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        // 计算转碳能源类型
        Map<Vehicles, String> carbonFuelTypeMap = new HashMap<>();

        // 计算净油耗
        Map<Vehicles, Double> consumption0Map = new HashMap<>();
        Map<Vehicles, Double> consumption1Map = new HashMap<>();
        Map<Vehicles, Double> consumption3Map = new HashMap<>();

        // 计算目标值
        Map<Vehicles, Double> target0Map = new HashMap<>();
        Map<Vehicles, Double> target1Map = new HashMap<>();
        Map<Vehicles, Double> target3Map = new HashMap<>();

        //计算新能源调节系数
        Map<Vehicles, Double> penetrationMap = new HashMap<>();
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
            String carbonEnergy = v.computeCarbonFuelType();
            carbonFuelTypeMap.put(v, carbonEnergy);

            double consumption0 = v.computeOilConsumption(convertionProvider,0);
            double consumption1 = v.computeOilConsumption(convertionProvider,1);
            double consumption3 = v.computeOilConsumption(convertionProvider,3);

            double target0 = v.computeTarget(targetProvider,0);
            double target1 = v.computeTarget(targetProvider,1);
            double target3 = v.computeTarget(targetProvider,3);

            double penetrationRate = vehicleService.computePenetrationRate(vehiclesList, v);

            double bonus = bonusProvider.calculateBonus(v);

            double credit0 = v.computeNetOilCredit(bonus, target0, consumption0);
            double credit1 = v.computeNetOilCredit(bonus, target1, consumption1);
            double credit3 = v.computeNetOilCredit(bonus, target3, consumption3);
            credit0Map.put(v, credit0);
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

            penetrationMap.put(v,penetrationRate);
            System.out.println("penetration rate : " + penetrationRate);
            bonusMap.put(v, bonus);

        }
        // 取数
        carbonFuelTypeCol.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(carbonFuelTypeMap.get(cellData.getValue())));
        energyConsumptionMethod0Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(consumption0Map.get(cellData.getValue()))
        );
        energyConsumptionMethod1Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(consumption1Map.get(cellData.getValue())));
        energyConsumptionMethod3Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(consumption3Map.get(cellData.getValue())));

        target0Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(target0Map.get(cellData.getValue())));
        target1Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(target1Map.get(cellData.getValue())));
        target3Col.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(target3Map.get(cellData.getValue())));

        penetrationRateCol.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(penetrationMap.get(cellData.getValue())));
        bonusCol.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(bonusMap.get(cellData.getValue())));

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
        showComputedView();
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
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

    /**
     * 根据车辆告警状态为单元格应用/清除错误样式，并处理提示组件。
     * 同时挂载悬浮提示和右键菜单（如有告警）。
     */
    private void applyErrorStyle(Vehicles v, TableCell<?,?> cell) {
        if (rowWarningMap.containsKey(v)) {
            cell.setStyle(CELL_ERROR_STYLE);
            attachHoverWarning(cell, v);
            attachRightClickWarning(cell, v);
        } else {
            cell.setStyle("");
            cell.setOnContextMenuRequested(null);
            cell.setContextMenu(null);
            cell.setTooltip(null);
        }
    }

    private void setupGvwAreaComboCell() {
        gvwAreaCol.setCellFactory(col -> new TableCell<Vehicles, String>() {
            private ComboBox<String> combo;
            private void createCombo(String carbonGroup, String currentValue) {
                combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setEditable(false); // 取消自由输入
                // 自定义下拉与按钮单元，展示“(空)”代表 null
                combo.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            setText(item == null ? "(空)" : item);
                        }
                    }
                });
                combo.setButtonCell(new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            setText(item == null ? "(空)" : item);
                        }
                    }
                });
                // 选项首位加入 null，供用户选择清空
                combo.getItems().clear();
                combo.getItems().add(null);
                try {
                    var options = commercialTargetService.listGvwAreasByCarbonGroup(carbonGroup);
                    combo.getItems().addAll(options);
                } catch (Exception ex) {
                    // 保留仅有的 null 选项
                }
                // 选择当前值（为 null 时可选到“(空)”占位）
                combo.getSelectionModel().select(currentValue);
                combo.setOnAction(e -> {
                    if (gvwCommitInProgress) return;
                    String sel = combo.getSelectionModel().getSelectedItem(); // 可能为 null
                    gvwCommitInProgress = true;
                    try {
                        commitEdit(sel);
                    } finally {
                        gvwCommitInProgress = false;
                    }
                });
                combo.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) { // focus lost
                        if (gvwCommitInProgress) return;
                        String sel = combo.getSelectionModel().getSelectedItem(); // 可能为 null
                        if (!Objects.equals(sel, getItem())) {
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
                        // 仅对重型车在质量段列应用错误样式/提示/右键
                        applyErrorStyle(v, this);
                    } else {
                        // 轻型车：质量段列不标红、不提示、不右键
                        setText(value);
                        setGraphic(null);
                        setStyle("");
                        setTooltip(null);
                        setOnContextMenuRequested(null);
                        setContextMenu(null);
                    }
                } else {
                    setText(value);
                    setGraphic(null);
                    if (v instanceof HeavyVehicle) {
                        // 仅重型车在质量段列显示错误样式/提示/右键
                        applyErrorStyle(v, this);
                    } else {
                        // 轻型车：保持干净
                        setStyle("");
                        setTooltip(null);
                        setOnContextMenuRequested(null);
                        setContextMenu(null);
                    }
                }
            }
        });
        // Add OnEditCommit handler to update HeavyVehicle and refresh table, show Tooltip for warnings (multiple tooltips supported)
        gvwAreaCol.setOnEditCommit(event -> {
            Vehicles v = event.getRowValue();
            if (v instanceof HeavyVehicle) {
                String newVal = event.getNewValue();
                String carbonGroup = v.getCarbonGroup();

                // 允许置空，但不短路：统一交给 service 判断（包括“总质量与质量段不能同时为空”等）
                if (newVal == null || newVal.isBlank()) {
                    ((HeavyVehicle) v).setGvwArea(null);
                } else {
                    ((HeavyVehicle) v).setGvwArea(newVal);
                }

                // 始终调用 service，让其返回权威结论
                String returnMessage = commercialTargetService.ifMatchGVMArea(carbonGroup, v.getGrossWeight(), ((HeavyVehicle) v).getGvwArea());

                if (!"ok".equalsIgnoreCase(returnMessage)) {
                    rowWarningMap.put(v, returnMessage);
                    showGvwWarning(v, returnMessage);
                } else {
                    rowWarningMap.remove(v);
                    hideGvwWarning(v);
                }
                vehicleTable.refresh();
            }
        });
    }

    private void setupYearComboCell(){
        // 允许的年份集合
        final List<Integer> allowedYears = Arrays.asList(2028, 2029, 2030);

        // 使用自定义 TableCell + ComboBox 实现，仅允许选择 3 个年份
        yearCol.setCellFactory(col -> new TableCell<Vehicles, Integer>() {
            private ComboBox<Integer> combo;

            private void createCombo(Integer current) {
                combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setEditable(false); // 只允许下拉选择
                combo.getItems().setAll(allowedYears);
                combo.getSelectionModel().select(current);
                combo.setOnAction(e -> {
                    Integer sel = combo.getSelectionModel().getSelectedItem();
                    commitEdit(sel);
                });
                combo.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV) {
                        Integer sel = combo.getSelectionModel().getSelectedItem();
                        if (!Objects.equals(sel, getItem())) {
                            commitEdit(sel);
                        }
                    }
                });
            }

            @Override
            public void startEdit() {
                super.startEdit();
                Vehicles v = getTableView().getItems().get(getIndex());
                createCombo(getItem());
                setText(null);
                setGraphic(combo);
                combo.requestFocus();
                combo.show();
                // 针对年份列也应用错误样式/提示（复用已有机制）
                applyErrorStyle(v, this);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() == null ? "" : String.valueOf(getItem()));
                setGraphic(null);
            }

            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    setTooltip(null);
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                    return;
                }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (isEditing()) {
                    createCombo(value);
                    setText(null);
                    setGraphic(combo);
                    applyErrorStyle(v, this);
                } else {
                    setText(value == null ? "" : String.valueOf(value));
                    setGraphic(null);
                    applyErrorStyle(v, this);
                }
            }
        });

        // 统一的提交与校验逻辑：只能是 2028/2029/2030
        yearCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Integer newYear = evt.getNewValue();
            // 写回 model
            v.setYear(newYear);

            String warn = null;
            if (newYear == null || !allowedYears.contains(newYear)) {
                warn = "年份不合法：只允许 2028 / 2029 / 2030";
            }

            if (warn != null) {
                rowWarningMap.put(v, warn);
            } else {
                // 仅在当前是年份相关报错时移除，避免覆盖其它类型告警
                String prev = rowWarningMap.get(v);
                if (prev != null && prev.startsWith("年份不合法")) {
                    rowWarningMap.remove(v);
                }
            }

            // 刷新以触发样式与提示
            vehicleTable.refresh();
        });

        // 更易点中编辑，适度加宽
        yearCol.setPrefWidth(90);
    }

    private void showGvwWarning(Vehicles v, String msg) {
        // 保留存储逻辑（行提示/样式依旧依赖 rowWarningMap），不再自动弹出浮窗
        if (msg != null && !msg.isEmpty()) {
            rowWarningMap.put(v, msg);
        }
        // 如需仍保留自动弹窗，可将本方法恢复为原实现
    }

    private void hideGvwWarning(Vehicles v) {
        Tooltip old = rowTooltipMap.remove(v);
        if (old != null) {
            try { old.hide(); } catch (Exception ignore) {}
        }
    }

    // 新增: 导入后批量校验GVW并显示警告（优化：仅变化行刷新/提示，支持清空）
    private void validateGvwAfterImport() {
        Set<Vehicles> changed = new HashSet<>();

        for (Vehicles v : vehiclesList) {
            if (v instanceof HeavyVehicle) {
                String gvw = ((HeavyVehicle) v).getGvwArea();
                String carbonGroup = v.getCarbonGroup();

                String msg = commercialTargetService.ifMatchGVMArea(carbonGroup, v.getGrossWeight(), gvw);

                if (!"ok".equalsIgnoreCase(msg)) {
                    String prev = rowWarningMap.put(v, msg);
                    if (!Objects.equals(prev, msg)) {
                        changed.add(v);
                    }
                } else {
                    if (rowWarningMap.remove(v) != null) {
                        changed.add(v);
                    }
                }
            }
        }

        if (!changed.isEmpty()) {
            for (Vehicles v : changed) {
                String msg = rowWarningMap.get(v);
                if (msg != null && !msg.isEmpty()) {
                    showGvwWarning(v, msg);
                } else {
                    hideGvwWarning(v);
                }
            }
            vehicleTable.refresh();
        }
    }

    // 新增: 轻型车导入后批量校验（测试质量 vs. 计算值）
// 优化版：轻型车导入后批量校验（仅变化行）
    private void validateLightAfterImport() {
        Set<Vehicles> changed = new HashSet<>();

        for (Vehicles v : vehiclesList) {
            if (v instanceof LightVehicle) {
                String msg = validateLightVehicle((LightVehicle) v);
                String prev = rowWarningMap.get(v);

                if (msg != null && !msg.isEmpty()) {
                    // 新增或信息变化才记录
                    if (!Objects.equals(prev, msg)) {
                        rowWarningMap.put(v, msg);
                        changed.add(v);
                    }
                } else {
                    // 原本有告警，现在通过
                    if (rowWarningMap.remove(v) != null) {
                        changed.add(v);
                    }
                }
            }
        }

        if (!changed.isEmpty()) {
            for (Vehicles v : changed) {
                String msg = rowWarningMap.get(v);
                if (msg != null && !msg.isEmpty()) {
                    showGvwWarning(v, msg);
                } else {
                    hideGvwWarning(v);
                }
            }
            vehicleTable.refresh();
        }
    }

    private String validateLightVehicle(LightVehicle lv) {
        if (lv == null) return null;

        Double testMass = lv.getTestMass();
        Integer curbWeight = lv.getCurbWeight();
        Integer grossWeight = lv.getGrossWeight();
        if (testMass == null) {
            if(curbWeight != null && grossWeight != null) {
                return null;
            }
            return "测试质量无法计算，请核实信息";
        }

        if(curbWeight == null || grossWeight == null){
            return null;
        }
        if (curbWeight > grossWeight) {
            return "测试质量无法计算，请核实信息";
        }

        // 根据车型(N1/M2)计算 goal_tm
        String carbonModel = lv.getCarbonModel();
        double curb = curbWeight.doubleValue();
        double gross = grossWeight.doubleValue();

        double ratio = "N1".equals(carbonModel) ? 0.15 : 0.28;
        double goal_tm = curb + 100.0 + (gross - curb - 100.0) * ratio;

        if (Math.abs(goal_tm - testMass) > 0.5) {
            TMRecommend.put(lv, goal_tm);
            return "测试质量与计算结果不符，计算时以测试质量为准。右键查看根据计算结果";
        }
        return null;
    }

    private void showBaseView() {
        // 基础信息列显示
        fuelTypeCol.setVisible(true);
        curbWeightCol.setVisible(true);
        grossWeightCol.setVisible(true);
        testMassCol.setVisible(true);
        gvwAreaCol.setVisible(true);
        energyCol.setVisible(true);
        salesCol.setVisible(true);
        carbonGroupCol.setVisible(true);

        // 计算类列隐藏
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

    private void showComputedView() {
        // 基础信息列隐藏
        fuelTypeCol.setVisible(false);
        curbWeightCol.setVisible(false);
        grossWeightCol.setVisible(false);
        testMassCol.setVisible(false);
        gvwAreaCol.setVisible(false);
        energyCol.setVisible(false);
        salesCol.setVisible(false);
        carbonGroupCol.setVisible(false);

        // 计算类列显示
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

    private void clearRowTooltips() {
        for (var tip : rowTooltipMap.values()) {
            try { tip.hide(); } catch (Exception ignore) {}
        }
        rowTooltipMap.clear();
    }

    private void resetStateForNewImport() {
        // 停止可能存在的编辑
        try {
            vehicleTable.edit(-1, null);
        } catch (Exception ignore) {
        }
        // 清数据容器
        vehicles.clear();
        vehiclesList.clear();
        rowWarningMap.clear();
        clearRowTooltips();
        gvwAreaCellMap.clear();
        // 还原列可见性
        showBaseView();
        // 刷新表格
        if (vehicleTable != null) vehicleTable.refresh();
    }


    /**
     * 在指定单元格上挂载“悬浮→展示提示”的 Tooltip（仅当该行存在告警时生效）。
     * 若为轻型车且存在推荐测试质量，则在提示文案中追加推荐值，
     * 并提示“右键可一键替换为计算值”。
     */
    private void attachHoverWarning(javafx.scene.control.TableCell<?, ?> cell, Vehicles v) {
        // 不移除右键菜单；允许同时存在悬浮与右键动作
        String defaultMsg = rowWarningMap.get(v);
        if (defaultMsg == null || defaultMsg.isEmpty()) {
            cell.setTooltip(null);
            return;
        }

        // 组合提示文案：默认告警 + 可选推荐值
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(warningMessageProvider.get(v, defaultMsg));

        Double rec = null;
        if (v instanceof LightVehicle) {
            rec = TMRecommend.get(v);
        }
        if (rec != null) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.0");
            msgBuilder.append("\n建议测试质量：").append(df.format(rec)).append(" kg");
            msgBuilder.append("\n(右键此单元格 → 使用计算值替换)");
        }

        Tooltip tip = new Tooltip();
        tip.setShowDelay(javafx.util.Duration.millis(180));
        tip.setHideDelay(javafx.util.Duration.millis(120));
        tip.setShowDuration(javafx.util.Duration.seconds(8));
        tip.setOnShowing(e -> {
            // 显示前动态刷新文案
            String latest = rowWarningMap.get(v);
            if (latest == null || latest.isEmpty()) {
                tip.setText("");
            } else {
                StringBuilder latestMsg = new StringBuilder();
                latestMsg.append(warningMessageProvider.get(v, latest));
                Double r = (v instanceof LightVehicle) ? TMRecommend.get(v) : null;
                if (r != null) {
                    java.text.DecimalFormat dff = new java.text.DecimalFormat("#,##0.0");
                    latestMsg.append("\n建议测试质量：").append(dff.format(r)).append(" kg");
                    latestMsg.append("\n(右键此单元格 → 使用计算值替换)");
                }
                tip.setText(latestMsg.toString());
            }
        });
        // 浅灰底 + 圆角
        tip.setStyle("-fx-background-radius: 8; -fx-background-color: rgba(245,245,245,0.98); -fx-text-fill: black; -fx-padding: 8; -fx-font-size: 12px;");
        cell.setTooltip(tip);

        // 确保右键菜单可用（带一键替换动作）
        attachRightClickWarning(cell, v);
    }

    /**
     * 在指定单元格上挂载“右键→查看提示/执行修正”的上下文菜单（仅当该行存在告警时生效）。
     * 若为轻型车且存在推荐测试质量，则提供一键替换动作。
     */
    private void attachRightClickWarning(javafx.scene.control.TableCell<?, ?> cell, Vehicles v) {
        cell.setOnContextMenuRequested(null);
        cell.setContextMenu(null);

        String defaultMsg = rowWarningMap.get(v);
        if (defaultMsg == null || defaultMsg.isEmpty()) {
            return; // 无告警不挂载
        }

        cell.setOnContextMenuRequested(e -> {
            String latest = rowWarningMap.get(v);
            if (latest == null || latest.isEmpty()) return;
            String finalMsg = warningMessageProvider.get(v, latest);

            javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem info = new javafx.scene.control.MenuItem(finalMsg);
            info.setDisable(true);
            menu.getItems().add(info);

            // 若存在推荐值，追加可执行操作
            if (v instanceof LightVehicle) {
                Double rec = TMRecommend.get(v);
                if (rec != null) {
                    java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.0");
                    javafx.scene.control.SeparatorMenuItem sep = new javafx.scene.control.SeparatorMenuItem();
                    javafx.scene.control.MenuItem apply = new javafx.scene.control.MenuItem("使用推荐测试质量（" + df.format(rec) + " kg）替换");
                    apply.setOnAction(ev -> applyRecommendedTestMass((LightVehicle) v, rec));
                    menu.getItems().addAll(sep, apply);
                }
            }

            menu.show(cell, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    /**
     * 将轻型车测试质量替换为推荐计算值，并触发一次校验与界面刷新。
     */
    private void applyRecommendedTestMass(LightVehicle lv, double rec) {
        lv.setTestMass(rec);
        String warn = validateLightVehicle(lv);
        if (warn != null && !warn.isEmpty()) {
            rowWarningMap.put(lv, warn);
            showGvwWarning(lv, warn);
        } else {
            rowWarningMap.remove(lv);
            hideGvwWarning(lv);
        }
        vehicleTable.refresh();
    }

    @FXML
    private void handleTreshold(ActionEvent event) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Threshold.fxml"));
        Parent root = loader.load();
        Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        scene.setRoot(root);
    }
}
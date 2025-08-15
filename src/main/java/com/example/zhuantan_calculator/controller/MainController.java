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
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.text.DecimalFormat;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
import java.util.stream.Collectors;
import java.util.function.Supplier;

public class MainController {

    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private Button templateButton;
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

    private List<Integer> errorRowIndex;
    private java.io.File lastImportedFile;

    private final ObservableList<Vehicles> vehicles = FXCollections.observableArrayList();
    private final List<Vehicles> vehiclesList = new ArrayList<>();
    // Map to track warning message for each row (vehicle)
    private final Map<Vehicles, String> warnYearMap = new HashMap<>();
    private final Map<Vehicles, String> warnHeavyGvwMap = new HashMap<>();
    private final Map<Vehicles, String> warnLightTestMassMap = new HashMap<>();
    private final Map<Vehicles, String> warnFuelTypeMap = new HashMap<>();
    private final Map<Vehicles, String> warnPHEVFuel1Map = new HashMap<>();
    private final Map<Vehicles, String> warnPHEVFuel2Map = new HashMap<>();
    private final List<Vehicles> crucialErrorList = new ArrayList<>();
    // 仅提示：能耗为0时的黄色提示（不进入crucialErrorList）
    private final Map<Vehicles, String> hintTotalEnergyZeroMap = new HashMap<>();
    private final Map<Vehicles, String> hintPHEV1EnergyZeroMap = new HashMap<>();
    private final Map<Vehicles, String> hintPHEV2EnergyZeroMap = new HashMap<>();
    // Anchor map for GVW area cells to show PopOver at the exact cell
    private final java.util.Map<Vehicles, TableCell<Vehicles, String>> gvwAreaCellMap = new HashMap<>();
    // Anchor map for LightVehicle testMass cells to show Tooltip at the testMass cell
    private final Map<Vehicles, TableCell<Vehicles, Double>> testMassCellMap = new HashMap<>();
    // Guard to prevent double commit when both onAction and focus listeners fire
    private boolean gvwCommitInProgress = false;
    // Map of test mass recommended
    private final Map<Vehicles, Double> TMRecommend = new HashMap<>();

    // 单元格错误高亮样式（淡红，不影响整行）
    private static final String CELL_ERROR_STYLE = "-fx-background-color: rgba(255,0,0,0.18);";
    private static final String CELL_HINT_STYLE = "-fx-background-color: #fff4cc;"; // 浅黄色提示

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

    // —— 统一的 Tooltip 工厂 ——
    private Tooltip buildWarningTooltip(Vehicles v, Supplier<String> msgSupplier) {
        Tooltip tip = new Tooltip();
        tip.setShowDelay(javafx.util.Duration.millis(180));
        tip.setHideDelay(javafx.util.Duration.millis(120));
        tip.setShowDuration(javafx.util.Duration.seconds(8));
        tip.setOnShowing(e -> {
            String latest = (msgSupplier == null) ? null : msgSupplier.get();
            if (latest == null || latest.isEmpty()) {
                tip.setText("");
            } else {
                tip.setText(warningMessageProvider.get(v, latest));
            }
        });
        tip.setStyle("-fx-background-radius: 8; -fx-background-color: rgba(245,245,245,0.98); -fx-text-fill: black; -fx-padding: 8; -fx-font-size: 12px;");
        return tip;
    }

    // 在所有错误Map均无此车辆时，才从 crucialErrorList 安全移除
    private void maybeRemoveFromCrucialIfNoOtherErrors(Vehicles v) {
        if (!warnYearMap.containsKey(v)
                && !warnHeavyGvwMap.containsKey(v)
                && !warnLightTestMassMap.containsKey(v)
                && !warnFuelTypeMap.containsKey(v)
                && !warnPHEVFuel1Map.containsKey(v)
                && !warnPHEVFuel2Map.containsKey(v)) {
            crucialErrorList.remove(v);
        }
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

        errorRowIndex = new ArrayList<>();

        vehicleTable.setItems(vehicles);
        // Set row factory to style entire row and show tooltip for warnings

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
                if (v instanceof LightVehicle && warnLightTestMassMap.containsKey(v)) {
                    setStyle(CELL_ERROR_STYLE);
                    Tooltip tip = buildWarningTooltip(v, () -> warnLightTestMassMap.get(v));
                    setTooltip(tip);
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
                String warn = validateLightVehicleMass((LightVehicle) v);
                if (warn != null && !warn.isEmpty()) {
                    warnLightTestMassMap.put(v, warn);
                } else {
                    warnLightTestMassMap.remove(v);
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
                if (v instanceof LightVehicle && warnLightTestMassMap.containsKey(v)) {
                    setStyle(CELL_ERROR_STYLE);
                    Tooltip tip = buildWarningTooltip(v, () -> warnLightTestMassMap.get(v));
                    setTooltip(tip);
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
                    warnHeavyGvwMap.put(v, warn);
                    crucialErrorList.add(v);
                } else {
                    warnHeavyGvwMap.remove(v);
                    maybeRemoveFromCrucialIfNoOtherErrors(v);
                }
            } else if (v instanceof LightVehicle) {
                warn = validateLightVehicleMass((LightVehicle) v);
                if (warn != null) {
                    warnLightTestMassMap.put(v, warn);
                    crucialErrorList.add(v);
                } else {
                    warnLightTestMassMap.remove(v);
                    maybeRemoveFromCrucialIfNoOtherErrors(v);
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
                            maybeRemoveFromCrucialIfNoOtherErrors(prev);
                        } catch (Exception ignore) {}
                        return;
                    }
                    Vehicles v = getTableView().getItems().get(getIndex());
                    // 仅记录轻型车的测试质量单元格作为锚点
                    if (v instanceof LightVehicle) {
                        testMassCellMap.put(v, this);
                    }
                    // 轻型车：该列为错误时标红
                    if (v instanceof LightVehicle && warnLightTestMassMap.containsKey(v)) {
                        setStyle(CELL_ERROR_STYLE);
                        Tooltip tip = buildWarningTooltip(v, () -> warnLightTestMassMap.get(v));
                        setTooltip(tip);
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
                String warn = validateLightVehicleMass((LightVehicle) v);
                if (warn != null && !warn.isEmpty()) {
                    warnLightTestMassMap.put(v, warn);
                    crucialErrorList.add(v);
                } else {
                    warnLightTestMassMap.remove(v);
                    maybeRemoveFromCrucialIfNoOtherErrors(v);
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
        // 让“燃料种类”可编辑
        fuelTypeCol.setEditable(true);
        setupFuelTypeComboCell();

        energyCol.setCellValueFactory(new PropertyValueFactory<>("energy"));
        PHEVFuel1Col.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel1()));
        PHEVFuel2Col.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel2()));

        PHEVFuel1EnergyCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel1Energy()));
        PHEVFuel2EnergyCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getPhevfuel2Energy()));

        // 允许编辑PHEV燃料子列，设置ComboCell
        PHEVFuel1Col.setEditable(true);
        PHEVFuel2Col.setEditable(true);
        setupPHEVFuel1ComboCell();
        setupPHEVFuel2ComboCell();

        salesCol.setCellValueFactory(new PropertyValueFactory<>("sales"));
        carbonGroupCol.setCellValueFactory(new PropertyValueFactory<>("carbonGroup"));


        useDecimalDisplay(energyCol, "#,##0.00");
        energyCol.setEditable(true);
        setupTotalEnergyEditableCell();

        PHEVFuel1EnergyCol.setEditable(true);
        PHEVFuel2EnergyCol.setEditable(true);
        setupPHEVFuel1EnergyEditableCell();
        setupPHEVFuel2EnergyEditableCell();

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
        templateButton.setOnAction(this::downloadTemplate);


        showBaseView();

    }

    private void setupFuelTypeComboCell() {
        // 合法燃料种类列表
        final List<String> legalFuelType = Arrays.asList("PHEV","FCV","BEV","汽油","柴油","天然气","甲醇");

        fuelTypeCol.setCellFactory(col -> new TableCell<Vehicles, String>() {
            private ComboBox<String> combo;

            private void createCombo(String current) {
                combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setEditable(false); // 仅允许在合法列表中选择
                combo.getItems().setAll(legalFuelType);
                combo.getSelectionModel().select(current);
                combo.setOnAction(e -> {
                    String sel = combo.getSelectionModel().getSelectedItem();
                    commitEdit(sel);
                });
                combo.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV) {
                        String sel = combo.getSelectionModel().getSelectedItem();
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
                    // 编辑态按需上色，这里保持与year一致：仅非编辑态提示
                } else {
                    setText(value);
                    setGraphic(null);
                    if (warnFuelTypeMap.containsKey(v)) {
                        setStyle(CELL_ERROR_STYLE);
                        Tooltip tip = buildWarningTooltip(v, () -> warnFuelTypeMap.get(v));
                        setTooltip(tip);
                    } else {
                        setStyle("");
                        setTooltip(null);
                    }
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
                }
            }
        });

        // 提交与校验：只允许 legalFuelType 列表中的值
        fuelTypeCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            String newVal = evt.getNewValue();
            // 写回 model
            v.setFuelType(newVal);

            String warn = null;
            if (newVal == null || !legalFuelType.contains(newVal)) {
                warn = "燃料种类不合法：只能选择 " + String.join("/", legalFuelType);
            }

            if (warn != null) {
                warnFuelTypeMap.put(v, warn);
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                // 仅在当前是燃料种类相关报错时移除
                String prev = warnFuelTypeMap.get(v);
                if (prev != null && prev.startsWith("燃料种类不合法")) {
                    warnFuelTypeMap.remove(v);
                }
                // 若切换为非PHEV，清理子燃料相关告警
                if (!"PHEV".equals(v.getFuelType())) {
                    warnPHEVFuel1Map.remove(v);
                    warnPHEVFuel2Map.remove(v);
                }
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }

            vehicleTable.refresh();
        });


        // 适度加宽
        fuelTypeCol.setPrefWidth(110);
    }
    private void validateFuelTypeAfterImport() {
        List<String> legalFuelType = Arrays.asList("PHEV","FCV","BEV","汽油","柴油","天然气","甲醇");
        for (Vehicles v : vehiclesList) {
            String ft = v.getFuelType();
            if (ft == null || !legalFuelType.contains(ft)) {
                warnFuelTypeMap.put(v, "燃料种类不合法：只能选择 " + String.join("/", legalFuelType));
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                warnFuelTypeMap.remove(v);
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }
        }
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
            // 记录本次导入的源文件，并清空错误行集合
            lastImportedFile = selectedFile;
            errorRowIndex.clear();
            ExcelReader reader = new ExcelReader();

            List<Map<String, String>> rawRows = reader.read(selectedFile);

            int rowIndex = 2;
            for (Map<String, String> row : rawRows) {
                try {
                    Vehicles v = VehicleFactory.createVehicleFromData(
                            parseYearSafe(emptyToNull(row.get("年份"))),
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
                    if (v != null) {
                        vehiclesList.add(v);
                    } else {
                        // createVehicleFromData 返回 null 视为错误行
                        errorRowIndex.add(rowIndex);
                    }

                } catch (Exception e) {
                    System.err.println("行解析失败 (第 " + rowIndex + " 行): " + row + ", 错误: " + e.getMessage());
                    errorRowIndex.add(rowIndex);
                }
                rowIndex++;
            }

            vehicles.setAll(vehiclesList);
            showBaseView();
            validateGvwAfterImport();
            validateLightAfterImport();
            validateYearAfterImport();
            validateFuelTypeAfterImport();
            validatePHEVSubFuelsAfterImport();
            validateZeroEnergyHintsAfterImport();
            vehicleTable.refresh();
    // 仅提示：总能耗(非PHEV)或PHEV子能耗为0时标黄提示（不加入crucialErrorList）

        }
    }

    private void validateZeroEnergyHintsAfterImport() {
        for (Vehicles v : vehiclesList) {
            // 总能耗：仅对非PHEV
            Double e = v.getEnergy();
            if (!"PHEV".equals(v.getFuelType()) && e != null && e == 0.0) {
                hintTotalEnergyZeroMap.put(v, "数值为0，请检查");
            } else {
                hintTotalEnergyZeroMap.remove(v);
            }
            // PHEV 子能耗 1
            Double e1 = v.getPhevfuel1Energy();
            if (e1 != null && e1 == 0.0) {
                hintPHEV1EnergyZeroMap.put(v, "数值为0，请检查");
            } else {
                hintPHEV1EnergyZeroMap.remove(v);
            }
            // PHEV 子能耗 2
            Double e2 = v.getPhevfuel2Energy();
            if (e2 != null && e2 == 0.0) {
                hintPHEV2EnergyZeroMap.put(v, "数值为0，请检查");
            } else {
                hintPHEV2EnergyZeroMap.remove(v);
            }
        }
    }


    private Integer parseYearSafe(String value) {
        if (value == null) return null;
        String s = value.trim();
        if (s.isEmpty()) return null;

        // 提取所有数字，兼容“2028年”“2028/07”“2028-01-01”等
        s = s.replaceAll("[^0-9]", "");
        if (s.isEmpty()) return null;

        // 仅使用前4位作为年份；不足4位视为无效
        if (s.length() >= 4) {
            s = s.substring(0, 4);
        } else {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }

    }

    private Integer parseIntSafe(String value) {
        if(value == null || value.isEmpty()) {
            return null;
        }
        if(value.contains("年")) {
            value = value.substring(0,4);
        }
        try {
            return (int)Double.parseDouble(value);
        } catch (Exception e) {
            return -1;
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

    // 在 calculate 之后提示错误行
    private void showErrorRowsAlertIfAny() {
        if (errorRowIndex == null || errorRowIndex.isEmpty()) return;
        String list = errorRowIndex.stream()
                .sorted()
                .map(Object::toString)
                .collect(Collectors.joining("，"));
        int n = errorRowIndex.size();
        String msg = "第" + list + "行、共" + n + "行出错了，请检查以防影响计算结果";
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("发现错误行");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
            // Auto-size 第一张表
            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            // ===== 追加第二个工作表：错误行（按导入文件原行复制，不写表头） =====
            if (lastImportedFile != null && lastImportedFile.exists() && errorRowIndex != null && !errorRowIndex.isEmpty()) {
                try (org.apache.poi.xssf.usermodel.XSSFWorkbook inWb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.FileInputStream(lastImportedFile))) {
                    org.apache.poi.ss.usermodel.Sheet inSheet = (inWb.getNumberOfSheets() > 0) ? inWb.getSheetAt(0) : null;
                    if (inSheet != null) {
                        org.apache.poi.ss.usermodel.DataFormatter fmt = new org.apache.poi.ss.usermodel.DataFormatter();
                        XSSFSheet errSheet = workbook.createSheet("错误行");

                        int outRowIdx = 0; // 不写任何表头，从第0行开始写
                        for (Integer excelRowNum : errorRowIndex) {
                            if (excelRowNum == null || excelRowNum < 1) continue;
                            int srcIdx = excelRowNum - 1; // Excel 可见行号 -> POI 索引
                            org.apache.poi.ss.usermodel.Row src = inSheet.getRow(srcIdx);
                            if (src == null) continue;

                            Row dst = errSheet.createRow(outRowIdx++);
                            short lastCell = src.getLastCellNum();
                            if (lastCell < 0) lastCell = 0;
                            for (int c = 0; c < lastCell; c++) {
                                org.apache.poi.ss.usermodel.Cell sc = src.getCell(c, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                                Cell dc = dst.createCell(c);
                                if (sc == null) {
                                    dc.setBlank();
                                    continue;
                                }
                                // 原封不动输出“显示值”即可（不改表头、不改列序）
                                switch (sc.getCellType()) {
                                    case STRING -> dc.setCellValue(sc.getStringCellValue());
                                    case NUMERIC -> dc.setCellValue(sc.getNumericCellValue());
                                    case BOOLEAN -> dc.setCellValue(sc.getBooleanCellValue());
                                    case FORMULA -> {
                                        // 为避免跨工作簿公式引用问题，这里直接写入公式的计算后显示值
                                        String display = fmt.formatCellValue(sc);
                                        dc.setCellValue(display);
                                    }
                                    case BLANK -> dc.setBlank();
                                    default -> {
                                        String display = fmt.formatCellValue(sc);
                                        dc.setCellValue(display);
                                    }
                                }
                            }
                        }

                        // 自动列宽
                        if (outRowIdx > 0) {
                            // 以第一条错误行的列数作为列宽基准
                            int colCount = workbook.getSheet("错误行").getRow(0).getLastCellNum();
                            for (int c = 0; c < colCount; c++) {
                                try { errSheet.autoSizeColumn(c); } catch (Exception ignore) {}
                            }
                        }
                    }
                } catch (Exception copyEx) {
                    // 复制错误行失败不阻断主导出
                    copyEx.printStackTrace();
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            javafx.scene.control.Alert ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            ok.setTitle("导出成功");
            ok.setHeaderText(null);
            ok.setContentText("文件已保存至：\n" + file.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            err.setTitle("导出失败");
            err.setHeaderText(null);
            err.setContentText("导出时发生错误：" + (e.getMessage() == null ? "未知错误" : e.getMessage()));
            err.showAndWait();
        }
    }

    @FXML
    private void calculateCredit() {
        // 计算前核验：若存在致命错误则阻断后续流程
        if (crucialErrorList != null && !crucialErrorList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("无法计算");
            alert.setHeaderText(null);
            alert.setContentText("有关键冲突没有解决");
            alert.showAndWait();
            crucialErrorList.stream()
                    .forEach(v ->
                            System.out.println(v.getModel()));
            return;
        }
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
        showErrorRowsAlertIfAny();
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            double result = Double.parseDouble(value);
            if(result >= 0){
                return result;
            }else{
                return 0.0;
            }
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
                    // 仅对重型车在质量段列应用错误样式/提示/右键（不再依赖 rowWarningMap）
                    if (warnHeavyGvwMap.containsKey(v)) {
                        setStyle(CELL_ERROR_STYLE);
                        Tooltip tip = buildWarningTooltip(v, () -> warnHeavyGvwMap.get(v));
                        setTooltip(tip);
                        attachRightClickWarning(this, v);
                    } else {
                        setStyle("");
                        setTooltip(null);
                        setOnContextMenuRequested(null);
                        setContextMenu(null);
                    }
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
                        if (warnHeavyGvwMap.containsKey(v)) {
                            setStyle(CELL_ERROR_STYLE);
                            Tooltip tip = buildWarningTooltip(v, () -> warnHeavyGvwMap.get(v));
                            setTooltip(tip);
                            attachRightClickWarning(this, v);
                        } else {
                            setStyle("");
                            setTooltip(null);
                            setOnContextMenuRequested(null);
                            setContextMenu(null);
                        }
                    } else {
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
                        if (warnHeavyGvwMap.containsKey(v)) {
                            setStyle(CELL_ERROR_STYLE);
                            Tooltip tip = buildWarningTooltip(v, () -> warnHeavyGvwMap.get(v));
                            setTooltip(tip);
                            attachRightClickWarning(this, v);
                        } else {
                            setStyle("");
                            setTooltip(null);
                            setOnContextMenuRequested(null);
                            setContextMenu(null);
                        }
                    } else {
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
                    warnHeavyGvwMap.put(v, returnMessage);
                    if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
                } else {
                    warnHeavyGvwMap.remove(v);
                    maybeRemoveFromCrucialIfNoOtherErrors(v);
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
                    setStyle(CELL_ERROR_STYLE);
                } else {
                    setText(value == null ? "" : String.valueOf(value));
                    setGraphic(null);
                    if (warnYearMap.containsKey(v)) {
                        setStyle(CELL_ERROR_STYLE);
                        Tooltip tip = buildWarningTooltip(v, () -> warnYearMap.get(v));
                        setTooltip(tip);
                    } else {
                        setStyle("");
                        setTooltip(null);
                    }
                    setOnContextMenuRequested(null);
                    setContextMenu(null);
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
                warnYearMap.put(v, warn);
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                // 仅在当前是年份相关报错时移除，避免覆盖其它类型告警
                String prev = warnYearMap.get(v);
                if (prev != null && prev.startsWith("年份不合法")) {
                    warnYearMap.remove(v);
                }
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }

            vehicleTable.refresh();
        });
        // 更易点中编辑，适度加宽
        yearCol.setPrefWidth(90);
    }


    // 新增: 导入后批量校验GVW并显示警告（优化：仅变化行刷新/提示，支持清空）
    private void validateGvwAfterImport() {
        for (Vehicles v : vehiclesList) {
            if (v instanceof HeavyVehicle) {
                String gvw = ((HeavyVehicle) v).getGvwArea();
                String carbonGroup = v.getCarbonGroup();

                String msg = commercialTargetService.ifMatchGVMArea(carbonGroup, v.getGrossWeight(), gvw);

                if (!"ok".equalsIgnoreCase(msg)) {
                    String prev = warnHeavyGvwMap.put(v, msg);
                    crucialErrorList.add(v);
                }
            }
        }
    }


    private void validateLightAfterImport() {
        for (Vehicles v : vehiclesList) {
            if (v instanceof LightVehicle) {
                String msg = validateLightVehicleMass((LightVehicle) v);
                String prev = warnLightTestMassMap.get(v);

                if (msg != null && !msg.isEmpty()) {
                    // 新增或信息变化才记录
                    if (!Objects.equals(prev, msg)) {
                        warnLightTestMassMap.put(v, msg);
                        crucialErrorList.add(v);
                    }
                }
            }
        }
    }

    private void validateYearAfterImport() {
        List<Integer> allowed = Arrays.asList(2028, 2029, 2030);
        for (Vehicles v : vehiclesList) {
            Integer y = v.getYear();
            if (!allowed.contains(y)) {
                warnYearMap.put(v, "年份不合法：只允许 2028 / 2029 / 2030");
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                warnYearMap.remove(v);
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }
        }
    }

    private String validateLightVehicleMass(LightVehicle lv) {
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

    // 校验PHEV子燃料合法性
    private void validatePHEVSubFuelsAfterImport() {
        List<String> legalPHEVFuelType = Arrays.asList("电","甲醇","汽油","柴油");
        for (Vehicles v : vehiclesList) {
            // 仅在主燃料类型为 PHEV 时校验
            if (!"PHEV".equals(v.getFuelType())) {
                warnPHEVFuel1Map.remove(v);
                warnPHEVFuel2Map.remove(v);
                maybeRemoveFromCrucialIfNoOtherErrors(v);
                continue;
            }
            String f1 = v.getPhevfuel1();
            String f2 = v.getPhevfuel2();

            boolean f1Bad = (f1 == null || !legalPHEVFuelType.contains(f1));
            if (f1Bad) {
                warnPHEVFuel1Map.put(v, "PHEV燃料1不合法：只能选择 " + String.join("/", legalPHEVFuelType));
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                warnPHEVFuel1Map.remove(v);
            }

            boolean f2Bad = false;
            if (f2 != null && !f2.isEmpty()) {
                if (!legalPHEVFuelType.contains(f2)) {
                    f2Bad = true;
                }
            }
            if (f2Bad) {
                warnPHEVFuel2Map.put(v, "PHEV燃料2不合法：只能选择 " + String.join("/", legalPHEVFuelType));
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                warnPHEVFuel2Map.remove(v);
            }

            if (!warnPHEVFuel1Map.containsKey(v) && !warnPHEVFuel2Map.containsKey(v)) {
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }
        }
    }
    // 设置PHEV燃料1 ComboCell
    private void setupPHEVFuel1ComboCell() {
        final List<String> legal = Arrays.asList("电","甲醇","汽油","柴油");
        PHEVFuel1Col.setCellFactory(col -> new TableCell<Vehicles, String>() {
            private ComboBox<String> combo;
            private void createCombo(String current) {
                combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setEditable(false);
                combo.getItems().setAll(legal);
                combo.getSelectionModel().select(current);
                combo.setOnAction(e -> commitEdit(combo.getSelectionModel().getSelectedItem()));
                combo.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV) {
                        String sel = combo.getSelectionModel().getSelectedItem();
                        if (!Objects.equals(sel, getItem())) commitEdit(sel);
                    }
                });
            }
            @Override public void startEdit() { super.startEdit(); createCombo(getItem()); setText(null); setGraphic(combo); combo.requestFocus(); combo.show(); }
            @Override public void cancelEdit() { super.cancelEdit(); setText(getItem()); setGraphic(null); }
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); setGraphic(null); setStyle(""); setTooltip(null); return; }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (isEditing()) {
                    createCombo(value); setText(null); setGraphic(combo);
                } else {
                    setText(value); setGraphic(null);
                    if (warnPHEVFuel1Map.containsKey(v)) { setStyle(CELL_ERROR_STYLE); setTooltip(buildWarningTooltip(v, () -> warnPHEVFuel1Map.get(v))); }
                    else { setStyle(""); setTooltip(null); }
                }
            }
        });
        PHEVFuel1Col.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            String newVal = evt.getNewValue();
            v.setPhevfuel1(newVal);

            if (!"PHEV".equals(v.getFuelType())) {
                // 非PHEV：清理本列与子燃料2报错，并尝试安全移出
                warnPHEVFuel1Map.remove(v);
                warnPHEVFuel2Map.remove(v);
                maybeRemoveFromCrucialIfNoOtherErrors(v);
                vehicleTable.refresh();
                return;
            }

            final List<String> legalList = legal;
            // 关键：读取 setter 后**规范化**的当前值，避免排序/去重导致的错位
            String f1 = v.getPhevfuel1();
            String f2 = v.getPhevfuel2();

            // 校验 fuel1 合法性
            if (f1 == null || !legalList.contains(f1)) {
                warnPHEVFuel1Map.put(v, "PHEV燃料1不合法：只能选择 " + String.join("/", legalList));
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                warnPHEVFuel1Map.remove(v);
            }

            // 校验 fuel2（仅合法性；与 fuel1 相同的情况由工厂/Setter 归一化）
            if (f2 != null && !f2.isEmpty() && !legalList.contains(f2)) {
                warnPHEVFuel2Map.put(v, "PHEV燃料2不合法：只能选择 " + String.join("/", legalList));
                if (!crucialErrorList.contains(v)) crucialErrorList.add(v);
            } else {
                warnPHEVFuel2Map.remove(v);
            }

            if (!warnPHEVFuel1Map.containsKey(v) && !warnPHEVFuel2Map.containsKey(v)) {
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }

            vehicleTable.refresh();
        });
        PHEVFuel1Col.setPrefWidth(110);
    }

    // 设置PHEV燃料2 ComboCell
    private void setupPHEVFuel2ComboCell() {
        final List<String> legal = Arrays.asList("电","甲醇","汽油","柴油");
        PHEVFuel2Col.setCellFactory(col -> new TableCell<Vehicles, String>() {
            private ComboBox<String> combo;
            private void createCombo(String current) {
                combo = new ComboBox<>();
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setEditable(false);
                combo.getItems().setAll(legal);
                combo.getSelectionModel().select(current);
                combo.setOnAction(e -> commitEdit(combo.getSelectionModel().getSelectedItem()));
                combo.focusedProperty().addListener((obs, oldV, newV) -> {
                    if (!newV) {
                        String sel = combo.getSelectionModel().getSelectedItem();
                        if (!Objects.equals(sel, getItem())) commitEdit(sel);
                    }
                });
            }
            @Override public void startEdit() { super.startEdit(); createCombo(getItem()); setText(null); setGraphic(combo); combo.requestFocus(); combo.show(); }
            @Override public void cancelEdit() { super.cancelEdit(); setText(getItem()); setGraphic(null); }
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); setGraphic(null); setStyle(""); setTooltip(null); return; }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (isEditing()) {
                    createCombo(value); setText(null); setGraphic(combo);
                } else {
                    setText(value); setGraphic(null);
                    if (warnPHEVFuel2Map.containsKey(v)) { setStyle(CELL_ERROR_STYLE); setTooltip(buildWarningTooltip(v, () -> warnPHEVFuel2Map.get(v))); }
                    else { setStyle(""); setTooltip(null); }
                }
            }
        });
        PHEVFuel2Col.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            String newVal = evt.getNewValue();
            v.setPhevfuel2(newVal);

            if (!"PHEV".equals(v.getFuelType())) {
                // 非PHEV：清理本列与子燃料1报错，并尝试安全移出
                warnPHEVFuel2Map.remove(v);
                warnPHEVFuel1Map.remove(v);
                maybeRemoveFromCrucialIfNoOtherErrors(v);
                vehicleTable.refresh();
                return;
            }

            final List<String> legalList = legal;
            // 关键：读取 setter 后**规范化**的当前值，避免排序/去重导致的错位
            String f1 = v.getPhevfuel1();
            String f2 = v.getPhevfuel2();

            String warn1 = null, warn2 = null;
            if (f1 == null || !legalList.contains(f1)) {
                warn1 = "PHEV燃料1不合法：只能选择 " + String.join("/", legalList);
            }
            if (f2 != null && !f2.isEmpty() && !legalList.contains(f2)) {
                warn2 = "PHEV燃料2不合法：只能选择 " + String.join("/", legalList);
            }

            if (warn1 != null) { warnPHEVFuel1Map.put(v, warn1); if (!crucialErrorList.contains(v)) crucialErrorList.add(v); } else { warnPHEVFuel1Map.remove(v); }
            if (warn2 != null) { warnPHEVFuel2Map.put(v, warn2); if (!crucialErrorList.contains(v)) crucialErrorList.add(v); } else { warnPHEVFuel2Map.remove(v); }

            if (!warnPHEVFuel1Map.containsKey(v) && !warnPHEVFuel2Map.containsKey(v)) {
                maybeRemoveFromCrucialIfNoOtherErrors(v);
            }

            // 新增：若从无到有创建了 PHEV 燃料2，通常其能耗会默认为 0；此处给出黄色提示（仅提示，不计入致命错误）
            Double e2 = v.getPhevfuel2Energy();
            if (e2 != null && e2 == 0.0) {
                hintPHEV2EnergyZeroMap.put(v, "数值为0，请检查");
            } else {
                hintPHEV2EnergyZeroMap.remove(v);
            }

            vehicleTable.refresh();
        });
        PHEVFuel2Col.setPrefWidth(110);
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
        PHEVFuel1Col.setVisible(true);
        PHEVFuel2Col.setVisible(true);
        PHEVFuel1EnergyCol.setVisible(true);
        PHEVFuel2EnergyCol.setVisible(true);

        // 计算类列隐藏
        energyConsumptionMethod0Col.setVisible(false);
        energyConsumptionMethod1Col.setVisible(false);
        energyConsumptionMethod3Col.setVisible(false);
        target0Col.setVisible(false);
        target1Col.setVisible(false);
        target3Col.setVisible(false);
        bonusCol.setVisible(false);
        penetrationRateCol.setVisible(false);
        carbonFuelTypeCol.setVisible(false);
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
        PHEVFuel1Col.setVisible(false);
        PHEVFuel2Col.setVisible(false);
        PHEVFuel1EnergyCol.setVisible(false);
        PHEVFuel2EnergyCol.setVisible(false);

        // 计算类列显示
        penetrationRateCol.setVisible(true);
        carbonFuelTypeCol.setVisible(true);
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


    private void resetStateForNewImport() {

        try {
            vehicleTable.edit(-1, null);
        } catch (Exception ignore) {
        }
        // 清数据容器
        vehicles.clear();
        vehiclesList.clear();

        gvwAreaCellMap.clear();
        // 还原列可见性
        showBaseView();
        // 刷新表格
        if (vehicleTable != null) vehicleTable.refresh();
    }



    /**
     * 在指定单元格上挂载“右键→查看提示/执行修正”的上下文菜单（仅当该行存在告警时生效）。
     * 若为轻型车且存在推荐测试质量，则提供一键替换动作。
     */
    private void attachRightClickWarning(TableCell<?, ?> cell, Vehicles v) {
        // 1) 只给轻型车绑定；重型车直接返回
        if (!(v instanceof LightVehicle)) {
            cell.setOnContextMenuRequested(null);
            cell.setContextMenu(null);
            return;
        }
        LightVehicle lv = (LightVehicle) v;

        // 清理旧的右键事件/菜单，避免单元格复用串台
        cell.setOnContextMenuRequested(null);
        cell.setContextMenu(null);

        // 2) 仅当“三个重量都存在 且 计算结果不符”时才绑定：
        //    该场景在 validateLightVehicleMass(...) 中会写入 TMRecommend
        if (lv.getCurbWeight() == null || lv.getGrossWeight() == null || lv.getTestMass() == null) {
            return; // 信息不全，不提供右键修复
        }
        final Double rec = TMRecommend.get(lv);
        if (rec == null) {
            return; // 没有推荐值，说明当前并非“计算不符”的错误场景
        }

        // 3) 绑定右键：显示“推荐测试质量：【值】（点击应用）”，点击后用推荐值替换
        cell.setOnContextMenuRequested(e -> {
            // 再次确认条件仍然满足（用户期间可能已修正）
            if (lv.getCurbWeight() == null || lv.getGrossWeight() == null || lv.getTestMass() == null) {
                return;
            }
            Double latestRec = TMRecommend.get(lv);
            if (latestRec == null) return;

            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.0");
            String label = "推荐测试质量：" + df.format(latestRec) + "（点击应用）";

            ContextMenu menu = new ContextMenu();
            MenuItem apply = new MenuItem(label);
            apply.setOnAction(ev -> {
                applyRecommendedTestMass(lv, latestRec);
            });
            menu.getItems().add(apply);

            menu.show(cell, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }

    /**
     * 将轻型车测试质量替换为推荐计算值，并触发一次校验与界面刷新。
     */
    private void applyRecommendedTestMass(LightVehicle lv, double rec) {
        lv.setTestMass(rec);
        String warn = validateLightVehicleMass(lv);
        if (warn != null && !warn.isEmpty()) {
            warnLightTestMassMap.put(lv, warn);
        } else {
            warnLightTestMassMap.remove(lv);
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

    public void downloadTemplate(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出模板");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        Window ownerWindow = importButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow);
        if (file != null) {
            exportTemplate(file);
        }
    }

    private void exportTemplate(File file) {
        // 建议将模板文件放在: src/main/resources/templates/zhuantan_template.xlsx
        // 打包后访问路径为类路径 "/templates/zhuantan_template.xlsx"
        final String resourcePath = "/templates/zhuantan_template.xlsx";

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("未在类路径中找到模板: " + resourcePath + "\n" +
                        "请确认已将模板放在 src/main/resources/templates/zhuantan_template.xlsx，并确保资源被打包进 jar。");
                javafx.scene.control.Alert warn = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                warn.setTitle("未找到模板");
                warn.setHeaderText(null);
                warn.setContentText("未在类路径中找到模板文件：" + resourcePath + "\n请确认模板已放入 src/main/resources/templates/zhuantan_template.xlsx 并随构建打包。");
                warn.showAndWait();
                return;
            }
            // 将模板二进制内容复制到用户选择的文件
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            javafx.scene.control.Alert ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            ok.setTitle("导出成功");
            ok.setHeaderText(null);
            ok.setContentText("模板已保存至：\n" + file.getAbsolutePath());
            ok.showAndWait();
        } catch (IOException e) {
            System.err.println("导出模板失败: " + e.getMessage());
            e.printStackTrace();
            javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            err.setTitle("导出失败");
            err.setHeaderText(null);
            err.setContentText("导出模板时发生错误：" + (e.getMessage() == null ? "未知错误" : e.getMessage()));
            err.showAndWait();
        }
    }

    // 总能耗：允许编辑；非PHEV且为0时黄标提示
    private void setupTotalEnergyEditableCell() {
        energyCol.setCellFactory(col -> new TableCell<Vehicles, Double>() {
            private final TextField tf = new TextField();
            private boolean committing = false;
            private void begin(Double cur) {
                tf.setText(cur == null ? "" : (cur % 1 == 0 ? String.valueOf(cur.intValue()) : String.valueOf(cur)));
                tf.setOnAction(e -> commitFromField());
                tf.focusedProperty().addListener((o, ov, nv) -> { if (!nv) commitFromField(); });
                setText(null); setGraphic(tf); tf.requestFocus(); tf.selectAll();
            }
            private void commitFromField() {
                if (committing) return; committing = true;
                try {
                    Double parsed = parseDoubleSafe(emptyToNull(tf.getText()));
                    commitEdit(parsed);
                } finally { committing = false; }
            }
            @Override public void startEdit() { super.startEdit(); begin(getItem()); }
            @Override public void cancelEdit() { super.cancelEdit(); setText(format(getItem())); setGraphic(null); }
            private String format(Double v) { return v == null ? null : (new java.text.DecimalFormat("0.###").format(v)); }
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); setGraphic(null); setStyle(""); setTooltip(null); return; }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (isEditing()) { begin(value); return; }
                setText(format(value)); setGraphic(null);
                if (hintTotalEnergyZeroMap.containsKey(v)) {
                    setStyle(CELL_HINT_STYLE);
                    setTooltip(buildWarningTooltip(v, () -> hintTotalEnergyZeroMap.get(v)));
                } else { setStyle(""); setTooltip(null); }
            }
        });
        energyCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Double parsed = parseDoubleSafe(evt.getNewValue() == null ? null : String.valueOf(evt.getNewValue()));
            v.setEnergy(parsed);
            if (!"PHEV".equals(v.getFuelType()) && parsed != null && parsed == 0.0) {
                hintTotalEnergyZeroMap.put(v, "数值为0，请检查");
            } else {
                hintTotalEnergyZeroMap.remove(v);
            }
            vehicleTable.refresh();
        });
    }

    // PHEV燃料1能耗：允许编辑；为0时黄标提示
    private void setupPHEVFuel1EnergyEditableCell() {
        PHEVFuel1EnergyCol.setCellFactory(col -> new TableCell<Vehicles, Double>() {
            private final TextField tf = new TextField();
            private boolean committing = false;
            private void begin(Double cur) {
                tf.setText(cur == null ? "" : (cur % 1 == 0 ? String.valueOf(cur.intValue()) : String.valueOf(cur)));
                tf.setOnAction(e -> commitFromField());
                tf.focusedProperty().addListener((o, ov, nv) -> { if (!nv) commitFromField(); });
                setText(null); setGraphic(tf); tf.requestFocus(); tf.selectAll();
            }
            private void commitFromField() { if (committing) return; committing = true; try { commitEdit(parseDoubleSafe(emptyToNull(tf.getText()))); } finally { committing = false; } }
            @Override public void startEdit() { super.startEdit(); begin(getItem()); }
            @Override public void cancelEdit() { super.cancelEdit(); setText(format(getItem())); setGraphic(null); }
            private String format(Double v) { return v == null ? null : (new java.text.DecimalFormat("0.###").format(v)); }
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); setGraphic(null); setStyle(""); setTooltip(null); return; }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (isEditing()) { begin(value); return; }
                setText(format(value)); setGraphic(null);
                if (hintPHEV1EnergyZeroMap.containsKey(v)) { setStyle(CELL_HINT_STYLE); setTooltip(buildWarningTooltip(v, () -> hintPHEV1EnergyZeroMap.get(v))); }
                else { setStyle(""); setTooltip(null); }
            }
        });
        PHEVFuel1EnergyCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Double parsed = parseDoubleSafe(evt.getNewValue() == null ? null : String.valueOf(evt.getNewValue()));
            v.setPhevfuel1Energy(parsed);
            if (parsed != null && parsed == 0.0) { hintPHEV1EnergyZeroMap.put(v, "数值为0，请检查"); }
            else { hintPHEV1EnergyZeroMap.remove(v); }
            vehicleTable.refresh();
        });
    }

    // PHEV燃料2能耗：允许编辑；为0时黄标提示
    private void setupPHEVFuel2EnergyEditableCell() {
        PHEVFuel2EnergyCol.setCellFactory(col -> new TableCell<Vehicles, Double>() {
            private final TextField tf = new TextField();
            private boolean committing = false;
            private void begin(Double cur) {
                tf.setText(cur == null ? "" : (cur % 1 == 0 ? String.valueOf(cur.intValue()) : String.valueOf(cur)));
                tf.setOnAction(e -> commitFromField());
                tf.focusedProperty().addListener((o, ov, nv) -> { if (!nv) commitFromField(); });
                setText(null); setGraphic(tf); tf.requestFocus(); tf.selectAll();
            }
            private void commitFromField() { if (committing) return; committing = true; try { commitEdit(parseDoubleSafe(emptyToNull(tf.getText()))); } finally { committing = false; } }
            @Override public void startEdit() { super.startEdit(); begin(getItem()); }
            @Override public void cancelEdit() { super.cancelEdit(); setText(format(getItem())); setGraphic(null); }
            private String format(Double v) { return v == null ? null : (new java.text.DecimalFormat("0.###").format(v)); }
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) { setText(null); setGraphic(null); setStyle(""); setTooltip(null); return; }
                Vehicles v = getTableView().getItems().get(getIndex());
                if (isEditing()) { begin(value); return; }
                setText(format(value)); setGraphic(null);
                if (hintPHEV2EnergyZeroMap.containsKey(v)) { setStyle(CELL_HINT_STYLE); setTooltip(buildWarningTooltip(v, () -> hintPHEV2EnergyZeroMap.get(v))); }
                else { setStyle(""); setTooltip(null); }
            }
        });
        PHEVFuel2EnergyCol.setOnEditCommit(evt -> {
            Vehicles v = evt.getRowValue();
            Double parsed = parseDoubleSafe(evt.getNewValue() == null ? null : String.valueOf(evt.getNewValue()));
            v.setPhevfuel2Energy(parsed);
            if (parsed != null && parsed == 0.0) { hintPHEV2EnergyZeroMap.put(v, "数值为0，请检查"); }
            else { hintPHEV2EnergyZeroMap.remove(v); }
            vehicleTable.refresh();
        });
    }

}

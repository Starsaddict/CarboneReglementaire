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
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.FileInputStream;
import org.apache.poi.ss.usermodel.DataFormatter;
import jakarta.persistence.EntityTransaction;

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

    private static Field[] fieldsForExport(Class<?> clazz) {
        // Include all declared fields, including id, and keep declaration order
        Field[] fields = clazz.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
        }
        return fields;
    }

    private static void writeSheet(Workbook wb, String sheetName, List<?> rows, Class<?> clazz) {
        Sheet sheet = wb.createSheet(sheetName);
        Field[] fields = fieldsForExport(clazz);

        java.util.List<Field> exportFields = java.util.Arrays.stream(fields)
                .filter(f -> !"id".equalsIgnoreCase(f.getName()))
                .collect(java.util.stream.Collectors.toList());

        // Header
        Row header = sheet.createRow(0);
        for (int i = 0; i < exportFields.size(); i++) {
            org.apache.poi.ss.usermodel.Cell c = header.createCell(i);
            c.setCellValue(exportFields.get(i).getName());
        }

        // Body
        int r = 1;
        for (Object rowObj : rows) {
            Row row = sheet.createRow(r++);
            for (int i = 0; i < exportFields.size(); i++) {
                Field f = exportFields.get(i);
                Object val = null;
                try {
                    val = f.get(rowObj);
                } catch (IllegalAccessException ignored) { }
                org.apache.poi.ss.usermodel.Cell c = row.createCell(i);
                if (val == null) {
                    c.setBlank();
                } else if (val instanceof Number) {
                    c.setCellValue(((Number) val).doubleValue());
                } else if (val instanceof Boolean) {
                    c.setCellValue((Boolean) val);
                } else {
                    c.setCellValue(String.valueOf(val));
                }
            }
        }

        // Autosize columns (safe upper bound)
        for (int i = 0; i < exportFields.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static <T> List<T> readSheet(Workbook wb, String sheetName, Class<T> clazz) {
        Sheet sheet = wb.getSheet(sheetName);
        if (sheet == null) return List.of();

        // Map header -> column index
        Row header = sheet.getRow(0);
        if (header == null) return List.of();
        int lastCol = header.getLastCellNum();
        java.util.Map<String,Integer> colIndex = new java.util.HashMap<>();
        for (int c = 0; c < lastCol; c++) {
            org.apache.poi.ss.usermodel.Cell hc = header.getCell(c);
            if (hc != null) {
                String name = hc.getStringCellValue();
                if (name != null && !name.isBlank()) {
                    colIndex.put(name.trim(), c);
                }
            }
        }

        Field[] fields = fieldsForExport(clazz);
        DataFormatter fmt = new DataFormatter();
        List<T> result = new java.util.ArrayList<>();

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            try {
                T obj = clazz.getDeclaredConstructor().newInstance();
                for (Field f : fields) {
                    String fieldName = f.getName();
                    // 默认跳过 id：避免与自增主键冲突
                    if ("id".equalsIgnoreCase(fieldName)) continue;

                    Integer cIdx = colIndex.get(fieldName);
                    if (cIdx == null) continue;
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cIdx);
                    String text = cell == null ? null : fmt.formatCellValue(cell).trim();
                    if (text == null || text.isEmpty()) {
                        // 写入 null
                        f.set(obj, null);
                    } else {
                        Class<?> t = f.getType();
                        try {
                            if (t == String.class) {
                                f.set(obj, text);
                            } else if (t == Integer.class || t == int.class) {
                                f.set(obj, Integer.valueOf(text.replace(",", "")));
                            } else if (t == Double.class || t == double.class) {
                                f.set(obj, Double.valueOf(text.replace(",", "")));
                            } else if (t == Boolean.class || t == boolean.class) {
                                f.set(obj, "true".equalsIgnoreCase(text) || "1".equals(text));
                            } else {
                                // 其他类型：先尝试直接设字符串，失败则忽略
                                try { f.set(obj, text); } catch (Exception ignore) {}
                            }
                        } catch (Exception parseEx) {
                            // 单元格内容与字段类型不匹配时，降级为 null
                            f.set(obj, null);
                        }
                    }
                }
                result.add(obj);
            } catch (Exception instEx) {
                // 跳过不可实例化的行
            }
        }
        return result;
    }

    private <T> void replaceAllInDb(Class<T> clazz, List<T> rows) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // 先清理持久化上下文，避免存在已管理的同类实体导致主键冲突
            em.clear();

            // 物理删除表中所有数据
            em.createQuery("DELETE FROM " + clazz.getSimpleName()).executeUpdate();
            em.flush();
            // 再次清理，确保 Session 中没有残留实体（避免 NonUniqueObjectException）
            em.clear();

            // 批量插入新数据，确保 id 为空（让 @GeneratedValue 重新生成）
            for (T row : rows) {
                nullifyId(row);
                em.persist(row);
            }

            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        }
    }

    private static void nullifyId(Object entity) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            Class<?> t = f.getType();
            if (t == Integer.class || t == Long.class) {
                f.set(entity, null);
            } else if (t == int.class) {
                f.setInt(entity, 0);
            } else if (t == long.class) {
                f.setLong(entity, 0L);
            }
        } catch (NoSuchFieldException ignore) {
            // 如果没有 id 字段，忽略
        } catch (Exception ex) {
            // 避免因个别实体设置 id 失败导致整体导入失败
        }
    }

    public void exportThreshold(ActionEvent actionEvent) {
        // Collect current data from the four tables
        List<CommercialTarget> targetRows = target.getItems();
        List<CarbonFactor> factorRows = factor.getItems();
        List<EnergyConversion> conversionRows = conversion.getItems();
        List<NewEnergyThreshold> thresholdRows = threshold.getItems();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出四张表为Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 工作簿 (*.xlsx)", "*.xlsx"));

        File file = chooser.showSaveDialog(choiceBox.getScene().getWindow());
        if (file == null) return; // 用户取消

        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {
            writeSheet(wb, "CommercialTarget", targetRows, CommercialTarget.class);
            writeSheet(wb, "CarbonFactor", factorRows, CarbonFactor.class);
            writeSheet(wb, "EnergyConversion", conversionRows, EnergyConversion.class);
            writeSheet(wb, "NewEnergyThreshold", thresholdRows, NewEnergyThreshold.class);

            wb.write(fos);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText(null);
            ok.setTitle("导出完成");
            ok.setContentText("已成功导出到：\n" + file.getAbsolutePath());
            ok.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText("导出失败");
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
    }

    public void importThreshold(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入Excel并覆盖四张表");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 工作簿 (*.xlsx)", "*.xlsx"));

        File file = chooser.showOpenDialog(choiceBox.getScene().getWindow());
        if (file == null) return; // 用户取消

        try (FileInputStream fis = new FileInputStream(file); Workbook wb = new XSSFWorkbook(fis)) {
            // 读取四个工作表
            List<CommercialTarget> targetRows = readSheet(wb, "CommercialTarget", CommercialTarget.class);
            List<CarbonFactor> factorRows = readSheet(wb, "CarbonFactor", CarbonFactor.class);
            List<EnergyConversion> conversionRows = readSheet(wb, "EnergyConversion", EnergyConversion.class);
            List<NewEnergyThreshold> thresholdRows = readSheet(wb, "NewEnergyThreshold", NewEnergyThreshold.class);

            // 事务性地覆盖数据库
            replaceAllInDb(CommercialTarget.class, targetRows);
            replaceAllInDb(CarbonFactor.class, factorRows);
            replaceAllInDb(EnergyConversion.class, conversionRows);
            replaceAllInDb(NewEnergyThreshold.class, thresholdRows);

            // 刷新 TableView
            target.setItems(FXCollections.observableArrayList(commercialTargetService.getAllCommercialTarget()));
            factor.setItems(FXCollections.observableArrayList(carbonFactorService.getAllCarbonFactor()));
            conversion.setItems(FXCollections.observableArrayList(energyConversionService.getAllEnergyConversion()));
            threshold.setItems(FXCollections.observableArrayList(newEnergyThresoldService.getAllEnergyThreshold()));

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText(null);
            ok.setTitle("导入完成");
            ok.setContentText("已覆盖数据库并刷新界面：\n" + file.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText("导入失败");
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
    }
}

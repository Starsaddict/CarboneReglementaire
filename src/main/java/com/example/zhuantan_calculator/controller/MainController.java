package com.example.zhuantan_calculator.controller;

import com.example.zhuantan_calculator.model.HeavyVehicle;
import com.example.zhuantan_calculator.model.LightVehicle;
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

import com.example.zhuantan_calculator.model.Vehicles;
import com.example.zhuantan_calculator.factory.VehicleFactory;
import com.example.zhuantan_calculator.util.ExcelReader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class MainController {


    @FXML private Button importButton;
    @FXML private TableView<Vehicles> vehicleTable;
    @FXML private TableColumn<Vehicles, Integer> yearCol ;
    @FXML private TableColumn<Vehicles, String> modelCol;
    @FXML private TableColumn<Vehicles, String> fuelTypeCol;
    @FXML private TableColumn<Vehicles, Integer> curbWeightCol;
    @FXML private TableColumn<Vehicles, Integer> grossWeightCol;
    @FXML private TableColumn<Vehicles, Double> testMassCol;
    @FXML private TableColumn<Vehicles, String> gvwAreaCol;
    @FXML private TableColumn<Vehicles, Double> energyCol;
    @FXML private TableColumn<Vehicles, Integer> salesCol;
    @FXML private TableColumn<Vehicles, String> carbonGroupCol;

    private final ObservableList<Vehicles> vehicles = FXCollections.observableArrayList();
    private final List<Vehicles> vehiclesList = new ArrayList<>();

    @FXML void initialize() {
        vehicleTable.setItems(vehicles);

        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));
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
        // 可选：数字右对齐
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
    }
}

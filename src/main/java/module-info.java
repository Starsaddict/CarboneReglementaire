module com.example.zhuantan_calculator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires jakarta.persistence;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens com.example.zhuantan_calculator.controller to javafx.fxml;
    opens com.example.zhuantan_calculator to javafx.fxml;
    opens com.example.zhuantan_calculator.model to javafx.base, org.hibernate.orm.core;

    exports com.example.zhuantan_calculator;
}

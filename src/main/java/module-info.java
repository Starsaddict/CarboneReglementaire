module com.example.zhuantan_calculator {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires static lombok;
    requires org.hibernate.orm.core;

    opens com.example.zhuantan_calculator.model to org.hibernate.orm.core;
    opens com.example.zhuantan_calculator to javafx.fxml;
    exports com.example.zhuantan_calculator;
}
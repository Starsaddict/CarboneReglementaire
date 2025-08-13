module com.example.zhuantan_calculator {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    // FXML 加载控制器需要反射访问
    opens com.example.zhuantan_calculator.controller to javafx.fxml;
    // 若有在 FXML 中直接引用主包类，这一行保留
    opens com.example.zhuantan_calculator to javafx.fxml;

    // 关键修改：允许 PropertyValueFactory 通过反射访问 model 包里的 getter
    // 同时继续对 Hibernate 开放
    opens com.example.zhuantan_calculator.model to javafx.base, org.hibernate.orm.core;

    // 如需对外暴露主包中的公开类，保留 exports
    exports com.example.zhuantan_calculator;


}
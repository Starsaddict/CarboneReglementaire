package com.example.zhuantan_calculator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        writeMarker("MainApp.start() - before FXML");
        writeMarker("before FXMLLoader");
        Parent root = FXMLLoader.load(getClass().getResource("/Main.fxml"));
        writeMarker("after FXMLLoader");

        writeMarker("MainApp.start() - after FXML");

        primaryStage.setTitle("转碳积分计算器");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        writeMarker("MainApp.start() - stage.show()");

    }

    private static String resolveDbPath() {
        String appDir = System.getenv("APPDIR");
        if (appDir != null && !appDir.isBlank()) {
            // jpackage 设置了 APPDIR
            return Paths.get(appDir, "app", "data", "vehicles.db").toAbsolutePath().toString();
        }
        try {
            Path codePath = Paths.get(MainApp.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            Path jarDir;
            if (java.nio.file.Files.isDirectory(codePath)) {
                // IDE运行，codePath = target/classes
                jarDir = codePath;
            } else {
                // 打包运行，codePath = app/zhuantan-calculator-1.0-SNAPSHOT.jar
                jarDir = codePath.getParent();
            }

            // app/data/vehicles.db（打包态） 或 project/data/vehicles.db（开发态）
            Path dbPath = jarDir.resolve("data").resolve("vehicles.db");

            // 如果在 IDE 下运行，还可以退回找项目根
            if (!java.nio.file.Files.exists(dbPath)) {
                Path projectRoot = jarDir.getParent().getParent(); // 回到项目根
                dbPath = projectRoot.resolve("vehicles.db");
            }

            return dbPath.toAbsolutePath().toString();
        } catch (Exception e) {
            return Paths.get("data", "vehicles.db").toAbsolutePath().toString();
        }
    }


    @Override
    public void init() {
        writeMarker("MainApp.init()");
        System.setProperty("db.path", resolveDbPath());
        System.out.println("[DB] path = " + System.getProperty("db.path"));
    }

    // 放到 MainApp.init() 里
    private void writeMarker(String phase) {
        try {
            String p = System.getProperty("app.marker.file");
            if (p != null && !p.isBlank()) {
                java.nio.file.Files.createDirectories(java.nio.file.Path.of(p).getParent());
                var line = java.time.Instant.now() + " - " + phase + System.lineSeparator();
                java.nio.file.Files.write(java.nio.file.Path.of(p), line.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (Exception ignore) {}
    }




    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
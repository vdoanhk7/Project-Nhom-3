package com.nhom3.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Điểm khởi đầu luôn là màn hình Đăng Nhập
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nhom3/client/view/login.fxml"));
        Parent root = loader.load();
        
        // Thiết lập các thuộc tính cho cửa sổ (Stage)
        primaryStage.setTitle("Hệ Thống Đấu Giá - Nhóm 3");
        primaryStage.setScene(new Scene(root));
        
        // Mẹo: Tạm thời khóa tính năng phóng to/thu nhỏ cửa sổ để màn hình Login không bị vỡ layout
        primaryStage.setResizable(false); 
        
        // Hiển thị cửa sổ
        primaryStage.show();
    }

    public static void main(String[] args) {
        ServerConnection serverPointer = ServerConnection.getInstance();
        boolean isConnected = false;
        while (!isConnected) {
            try {
                serverPointer.connect();
                isConnected = true; // Kết nối thành công, thoát vòng lặp
            } catch (Exception e) {
                System.out.println("Không thể kết nối đến server. Đang thử lại...");
            }
        }
        launch(args);
    }
}
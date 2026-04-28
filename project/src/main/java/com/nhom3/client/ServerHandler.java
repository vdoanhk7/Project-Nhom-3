package com.nhom3.client;

import com.google.gson.Gson;
import com.nhom3.shared.network.Response;

public class ServerHandler extends Thread {
    private final ServerConnection serverConnection;
    private boolean isListening;

    public ServerHandler() {
        this.serverConnection = ServerConnection.getInstance();
        isListening = true;
    }

    @Override
    public void run() {
        while (isListening) {
            try {
                String jsonResponse;
                // Phần điều kiện phải sửa 
                if ((jsonResponse = serverConnection.receiveResponse()) != null) {
                    Response response = new Gson().fromJson(jsonResponse, Response.class);
                    //Thêm các case khác
                    //Update xử lý payload
                    switch (response.getStatus()) {
                        case "SUCCESS":
                            System.out.println("Thành công: " + response.getMessage());
                            break;
                        case "ERROR":
                            System.out.println("Lỗi: " + response.getMessage());
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                // serverConnection.connect();
                // Thứ kết nôi lại server nếu không được :
                System.out.println("Lỗi khi nhận phản hồi từ server. Đang thử kết nối lại...");
                isListening = false;
            }
        }
    }
}

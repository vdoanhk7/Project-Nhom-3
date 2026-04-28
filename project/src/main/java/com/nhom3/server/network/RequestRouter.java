package com.nhom3.server.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.model.user.User;
import com.nhom3.shared.network.Request;
import com.nhom3.shared.network.Response;

public class RequestRouter {
    private Gson gson = new Gson();
    private AuthService authService = new AuthService();

    public Response route(Request request) {
        switch (request.getAction()) {
            case "LOGIN":
                return new Response("SUCCESS", "Đăng nhập thành công", null);
            case "STH":
                return new Response("ERROR", "STH thất bại", null);
            // Thêm các case khác: "REGISTER", "PLACE_BID", "GET_MARKET"...
            default:
                return new Response("ERROR", "Action không hợp lệ", null);
        }
    }

    private Response handleLogin(String payload) {
        // Parse payload (ví dụ payload gửi lên là JSON {"username":"...", "password":"..."})
        JsonObject json = gson.fromJson(payload, JsonObject.class);
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();

        User user = authService.login(username, password);
        if (user != null) {
            // Convert User object thành chuỗi JSON để gửi về
            return new Response("SUCCESS", "Đăng nhập thành công", gson.toJson(user));
        } else {
            return new Response("ERROR", "Sai tài khoản hoặc mật khẩu", null);
        }
    }
}
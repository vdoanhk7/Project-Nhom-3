package com.nhom3.server.network;

import com.google.gson.Gson;
import com.nhom3.shared.network.Request;
import com.nhom3.shared.network.Response;
import java.io.*;
import java.net.Socket;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


public class ClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();
    private RequestRouter router = new RequestRouter(); // Chuyển hướng xử lý

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            log.error("Lỗi khi khởi tạo stream cho client: ", e);
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // 1. Nhận Request JSON từ Client
                Request request = gson.fromJson(inputLine, Request.class);
                log.info("[Server Nhận]: {}", request.getAction());
                
                // 2. Xử lý logic thông qua Router
                Response response = router.route(request);
                
                // 3. Trả Response về cho Client
                sendResponse(response);
            }
        } catch (IOException e) {
            log.info("Client ngắt kết nối.");
        } finally {
            AuctionServer.clients.remove(this);
        }
    }

    public void sendResponse(Response response) {
        out.println(gson.toJson(response));
    }
}
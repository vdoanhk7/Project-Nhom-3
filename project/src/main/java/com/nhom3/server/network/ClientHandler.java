package com.nhom3.server.network;

import java.io.*;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.server.service.AuthService;
import com.nhom3.server.service.AuctionService;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final PacketDispatcher dispatcher;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.authService = new AuthService();
        this.auctionService = new AuctionService();
        this.dispatcher = new PacketDispatcher(this.authService, this.auctionService);
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            String line;
            Gson gson = new Gson();

            while ((line = in.readLine()) != null) {
                Packet request = gson.fromJson(line, Packet.class);
                System.out.println("[SERVER - RAW RECEIVE] Vừa nhận được gói tin loại: " + request.getType());

                // Sử dụng Dispatcher thay vì Switch Case khổng lồ
                Packet response = dispatcher.dispatch(request, gson);

                if (response != null) {
                    out.write(gson.toJson(response));
                    out.newLine();
                    out.flush();
                }
            }
        } catch (IOException e) {
            logger.error("Client ngắt kết nối");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ex) {
            }
        }
    }
}
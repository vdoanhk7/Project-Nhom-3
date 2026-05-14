package com.nhom3.server.network.liveUpdate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.nhom3.server.network.ClientHandler;
import java.io.IOException;

public class Announcer {
    private static Announcer instance;
    private final ConcurrentHashMap<Integer, Set<AuctionObserver>> clientInAuctions;

    private Announcer() {
        this.clientInAuctions = new ConcurrentHashMap<>();
    }

    public synchronized static Announcer getInstance() {
        if (instance == null) {
            instance = new Announcer();
        }
        return instance;
    }

    public void addObserver(int auctionId, ClientHandler client) {
        AuctionObserver observer = new ScreenObserver(client);
        clientInAuctions.get(auctionId).add(observer);
    }

    public void removeObserver(int auctionId, ClientHandler client) {
        clientInAuctions.get(auctionId).removeIf(observer -> observer.client == client);
    }

    // public void setsUpdateType(int auctionId, int clientId, String type) {
    //     for (AuctionObserver observer : clientInAuctions.get(auctionId)) {
    //         if (client.id == clientId) {
    //             //observer = new ObserverType(client)
    //         }
    //     }
    // }

    public void notify(int auctionId, double amount) {
        for (AuctionObserver observer : clientInAuctions.get(auctionId)) {
            try {
                observer.update(amount);
            } catch (IOException e) {
                // Client mat ket noi voi server
                removeObserver(auctionId, observer.client);
            }
        }
    }
}

package com.nhom3.server.network.liveUpdate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.network.payload.BidHistoryResponsePayload;
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
        clientInAuctions
                .computeIfAbsent(auctionId, key -> new CopyOnWriteArraySet<>())
                .add(observer);
    }

    public void removeObserver(int auctionId, ClientHandler client) {
        Set<AuctionObserver> observers = clientInAuctions.get(auctionId);
        if (observers == null) {
            return;
        }
        observers.removeIf(observer -> observer.client == client);
        if (observers.isEmpty()) {
            clientInAuctions.remove(auctionId, observers);
        }
    }

    public void notify(int auctionId, double amount) {
        Set<AuctionObserver> observers = clientInAuctions.get(auctionId);
        if (observers == null || observers.isEmpty()) {
            return;
        }
        for (AuctionObserver observer : observers) {
            try {
                observer.update(auctionId, amount);
            } catch (IOException e) {
                // Client mat ket noi voi server
                removeObserver(auctionId, observer.client);
            }
        }
    }

    public void notifyAuctionSnapshot(
            Auction auction,
            String eventType,
            String message,
            BidHistoryResponsePayload.SimpleBid latestBid) {
        if (auction == null) {
            return;
        }

        Set<AuctionObserver> observers = clientInAuctions.get(auction.getId());
        if (observers == null || observers.isEmpty()) {
            return;
        }
        for (AuctionObserver observer : observers) {
            try {
                if (observer instanceof ScreenObserver screenObserver) {
                    screenObserver.notifySnapshot(auction, eventType, message, latestBid);
                } else {
                    double highestPrice = auction.getItem() != null ? auction.getItem().getCurHighest() : 0;
                    observer.update(auction.getId(), highestPrice);
                }
            } catch (IOException e) {
                removeObserver(auction.getId(), observer.client);
            }
        }
    }

    public void notifyAuctionCancelled(int auctionId, String message) {
        notifyAuctionClosed(auctionId, message, false);
    }

    public void notifyAuctionDealCancelled(int auctionId, String message) {
        notifyAuctionClosed(auctionId, message, true);
    }

    private void notifyAuctionClosed(int auctionId, String message, boolean dealCancelled) {
        Set<AuctionObserver> observers = clientInAuctions.get(auctionId);
        if (observers == null || observers.isEmpty()) {
            return;
        }
        for (AuctionObserver observer : observers) {
            try {
                if (observer instanceof ScreenObserver screenObserver) {
                    if (dealCancelled) {
                        screenObserver.notifyDealCancelled(auctionId, message);
                    } else {
                        screenObserver.notifyCancelled(auctionId, message);
                    }
                }
            } catch (IOException e) {
                removeObserver(auctionId, observer.client);
            }
        }
    }
}

package com.nhom3.server.network.liveUpdate;

import com.nhom3.server.network.ClientHandler;

public abstract class AuctionObserver implements AnnouncerListener {
    public ClientHandler client;

    public AuctionObserver(ClientHandler client) {
        this.client = client;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof AuctionObserver))
            return false;
        AuctionObserver other = (AuctionObserver) obj;
        return this.client == other.client;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(client);
    }
}

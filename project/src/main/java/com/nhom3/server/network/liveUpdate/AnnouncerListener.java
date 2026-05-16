package com.nhom3.server.network.liveUpdate;

import java.io.IOException;

interface AnnouncerListener {
    void update(int auctionId, double updatePrice) throws IOException;
}

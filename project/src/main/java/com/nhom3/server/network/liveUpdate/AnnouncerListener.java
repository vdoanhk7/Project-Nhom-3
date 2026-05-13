package com.nhom3.server.network.liveUpdate;

import java.io.IOException;

interface AnnouncerListener {
    void update(double UpdatePrice) throws IOException;
}
package com.nhom3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nhom3.shared.model.Entity;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.item.Art;
import com.nhom3.shared.model.item.Electronics;
import com.nhom3.shared.model.item.Vehicle;
import com.nhom3.shared.model.user.Admin;
import com.nhom3.shared.model.user.Bidder;
import com.nhom3.shared.model.user.Seller;
import com.nhom3.shared.model.user.UserContact;
import com.nhom3.shared.model.user.UserInfo;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrintInfoPolymorphismTest {

    @Test
    void printInfoUsesConcreteEntityImplementations() {
        UserInfo userInfo = new UserInfo("demo", "secret", "Demo User");
        UserContact contact = new UserContact("demo@example.com", "0123456789");

        List<Entity> entities = List.of(
                new Art(1, "Painting", 1_000_000),
                new Electronics(2, "Laptop", 12_000_000),
                new Vehicle(3, "Motorbike", 25_000_000),
                new Bidder(4, userInfo, contact),
                new Seller(5, userInfo, contact),
                new Admin(6, userInfo, contact),
                new Auction(7, new Art(8, "Statue", 3_000_000),
                        LocalDateTime.now(), LocalDateTime.now().plusDays(1)));

        List<String> infos = entities.stream().map(Entity::printInfo).toList();

        assertTrue(infos.get(0).startsWith("Art["));
        assertTrue(infos.get(1).startsWith("Electronics["));
        assertTrue(infos.get(2).startsWith("Vehicle["));
        assertTrue(infos.get(3).startsWith("Bidder["));
        assertTrue(infos.get(4).startsWith("Seller["));
        assertTrue(infos.get(5).startsWith("Admin["));
        assertTrue(infos.get(6).startsWith("Auction["));
    }
}

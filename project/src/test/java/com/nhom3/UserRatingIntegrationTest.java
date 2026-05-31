package com.nhom3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.dao.UserDAOImpl;
import com.nhom3.shared.model.user.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserRatingIntegrationTest extends IntegrationTestBase {
    private AuctionDAOImpl auctionDAO;

    @BeforeEach
    void seedRatingData() throws Exception {
        auctionDAO = new AuctionDAOImpl();
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.executeUpdate("INSERT INTO users (id, username, password, full_name, email, phone, role) VALUES "
                    + "(1, 'seller01', 'x', 'Seller One', 'seller1@example.com', '0900000001', 'SELLER'),"
                    + "(2, 'bidder01', 'x', 'Bidder One', 'bidder1@example.com', '0900000002', 'BIDDER'),"
                    + "(3, 'bidder02', 'x', 'Bidder Two', 'bidder2@example.com', '0900000003', 'BIDDER'),"
                    + "(4, 'seller02', 'x', 'Seller Two', 'seller2@example.com', '0900000004', 'SELLER')");
            stmt.executeUpdate("INSERT INTO items (id, seller_id, name, start_price, cur_highest, item_type) VALUES "
                    + "(10, 1, 'Paid item', 100, 200, 'ART'),"
                    + "(11, 1, 'Second paid item', 100, 300, 'ART'),"
                    + "(12, 1, 'Unpaid item', 100, 200, 'ART'),"
                    + "(13, 4, 'Self item', 100, 200, 'ART')");
            stmt.executeUpdate("INSERT INTO auctions "
                    + "(id, item_id, start_time, end_time, bid_step, status, highest_bidder_id) VALUES "
                    + "(100, 10, DATEADD('DAY', -2, CURRENT_TIMESTAMP), "
                    + "DATEADD('DAY', -1, CURRENT_TIMESTAMP), 10, 'PAID', 2),"
                    + "(101, 11, DATEADD('DAY', -2, CURRENT_TIMESTAMP), "
                    + "DATEADD('DAY', -1, CURRENT_TIMESTAMP), 10, 'PAID', 3),"
                    + "(102, 12, DATEADD('DAY', -2, CURRENT_TIMESTAMP), "
                    + "DATEADD('DAY', -1, CURRENT_TIMESTAMP), 10, 'FINISHED', 2),"
                    + "(103, 13, DATEADD('DAY', -2, CURRENT_TIMESTAMP), "
                    + "DATEADD('DAY', -1, CURRENT_TIMESTAMP), 10, 'PAID', 4)");
        }
    }

    @Test
    void paidWinningBuyerCanRateSellerOnlyOnce() throws Exception {
        assertTrue(auctionDAO.rateSeller(100, 2, 5));
        assertFalse(auctionDAO.rateSeller(100, 2, 4));

        try (PreparedStatement stmt = h2Connection.prepareStatement(
                "SELECT stars FROM seller_ratings WHERE auction_id = ? AND buyer_id = ?")) {
            stmt.setInt(1, 100);
            stmt.setInt(2, 2);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt("stars"));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void ratingRejectsNonWinnerUnpaidAuctionAndSelfRating() {
        assertFalse(auctionDAO.rateSeller(100, 3, 4));
        assertFalse(auctionDAO.rateSeller(102, 2, 4));
        assertFalse(auctionDAO.rateSeller(103, 4, 4));
        assertFalse(auctionDAO.rateSeller(100, 2, 6));
    }

    @Test
    void sellerProfileReceivesAverageRating() {
        assertTrue(auctionDAO.rateSeller(100, 2, 5));
        assertTrue(auctionDAO.rateSeller(101, 3, 3));

        User seller = new UserDAOImpl().getById(1);

        assertEquals(4.0, seller.getUserRatingAverage(), 0.001);
        assertEquals(2, seller.getUserRatingCount());
    }
}

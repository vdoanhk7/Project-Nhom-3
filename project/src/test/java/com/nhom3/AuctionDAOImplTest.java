package com.nhom3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.nhom3.server.dao.AuctionDAOImpl;
import com.nhom3.server.db.DbConnection;
import com.nhom3.shared.model.auction.Auction;
import com.nhom3.shared.model.auction.BidTransaction;
import com.nhom3.shared.model.auction.StatusOfAuction;
import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.model.user.Bidder;

public class AuctionDAOImplTest {

    private AuctionDAOImpl auctionDAO;

    private Connection conn;
    private PreparedStatement stmt1;
    private PreparedStatement stmt2;
    private ResultSet rs;

    @BeforeEach
    void setUp() throws Exception {
        auctionDAO = new AuctionDAOImpl();

        conn = mock(Connection.class);
        stmt1 = mock(PreparedStatement.class);
        stmt2 = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);
    }

    @Test
    void testCreateAuctionSuccess() throws Exception {
        Item mockItem = mock(Item.class);
        when(mockItem.getId()).thenReturn(1);

        Auction auction = new Auction(0, mockItem, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        auction.setStatus(StatusOfAuction.OPEN);

        try (MockedStatic<DbConnection> mockedDb = Mockito.mockStatic(DbConnection.class)) {
            mockedDb.when(DbConnection::getConnection).thenReturn(conn);
            when(conn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(stmt1);
            when(conn.prepareStatement(anyString())).thenReturn(stmt2);
            when(stmt2.executeUpdate()).thenReturn(1);
            when(stmt1.executeUpdate()).thenReturn(1);
            when(stmt1.getGeneratedKeys()).thenReturn(rs);
            when(rs.next()).thenReturn(true);
            when(rs.getInt(1)).thenReturn(100);

            boolean result = auctionDAO.createAuction(auction);

            assertTrue(result);
            assertEquals(100, auction.getId());
            verify(stmt1).setInt(1, 1);
            verify(stmt1).setDouble(4, auction.getBidStep());
            verify(stmt1).setString(5, "OPEN");
            verify(conn).commit();
        }
    }

    @Test
    void testUpdateHighestBidSuccess() throws Exception {
        try (MockedStatic<DbConnection> mockedDb = Mockito.mockStatic(DbConnection.class)) {
            mockedDb.when(DbConnection::getConnection).thenReturn(conn);

            // It runs 2 statements
            when(conn.prepareStatement(anyString())).thenReturn(stmt1, stmt2);

            when(stmt1.executeUpdate()).thenReturn(1); // item update success
            when(stmt2.executeUpdate()).thenReturn(1); // auction update success

            boolean result = auctionDAO.updateHighestBid(1, 10, 500.0);

            assertTrue(result);
            verify(conn).setAutoCommit(false);
            verify(conn).commit();

            verify(stmt1).setInt(1, 10);
            verify(stmt1).setDouble(2, 500.0);
            verify(stmt1).setInt(3, 1);
            verify(stmt1).setDouble(4, 500.0);

            verify(stmt2).setInt(1, 10);
            verify(stmt2).setInt(2, 1);
        }
    }

    @Test
    void testSaveBidTransactionSuccess() throws Exception {
        Bidder bidder = mock(Bidder.class);
        when(bidder.getId()).thenReturn(1);
        BidTransaction bid = new BidTransaction(0, bidder, 100.0, LocalDateTime.now(), "Test");

        try (MockedStatic<DbConnection> mockedDb = Mockito.mockStatic(DbConnection.class)) {
            mockedDb.when(DbConnection::getConnection).thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(stmt1);
            when(stmt1.executeUpdate()).thenReturn(1);

            boolean result = auctionDAO.saveBidTransaction(bid, 10);

            assertTrue(result);
            verify(stmt1).setInt(1, 10);
            verify(stmt1).setInt(2, 1);
            verify(stmt1).setDouble(3, 100.0);
        }
    }

    @Test
    void testCancelAutoBidAlreadyMissingStillSucceeds() throws Exception {
        try (MockedStatic<DbConnection> mockedDb = Mockito.mockStatic(DbConnection.class)) {
            mockedDb.when(DbConnection::getConnection).thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(stmt1);
            when(stmt1.executeUpdate()).thenReturn(0);

            boolean result = auctionDAO.cancelAutoBid(1, 10);

            assertTrue(result);
            verify(stmt1).setInt(1, 1);
            verify(stmt1).setInt(2, 10);
        }
    }

    @Test
    void testGetAuctionByIdFound() throws Exception {
        try (MockedStatic<DbConnection> mockedDb = Mockito.mockStatic(DbConnection.class)) {
            mockedDb.when(DbConnection::getConnection).thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(stmt1);
            when(stmt1.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getString("item_type")).thenReturn("ELECTRONICS");
            when(rs.getInt("item_id")).thenReturn(5);
            when(rs.getString("item_name")).thenReturn("Laptop");
            when(rs.getDouble("start_price")).thenReturn(1000.0);
            when(rs.getDouble("cur_highest")).thenReturn(1200.0);
            when(rs.getDouble("bid_step")).thenReturn(200.0);
            when(rs.getTimestamp("start_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            when(rs.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
            when(rs.getString("status")).thenReturn("RUNNING");
            when(rs.getInt("highest_bidder_id")).thenReturn(2);

            Auction auction = auctionDAO.getAuctionById(1);

            assertNotNull(auction);
            assertEquals(1, auction.getId());
            assertEquals(5, auction.getItem().getId());
            assertEquals(200.0, auction.getBidStep());
            assertEquals(StatusOfAuction.RUNNING, auction.getStatus());
            assertEquals(2, auction.getHighestBidder().getId());
        }
    }

    @Test
    void testGetAuctionByIdNotFound() throws Exception {
        try (MockedStatic<DbConnection> mockedDb = Mockito.mockStatic(DbConnection.class)) {
            mockedDb.when(DbConnection::getConnection).thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(stmt1);
            when(stmt1.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(false);

            Auction auction = auctionDAO.getAuctionById(99);

            assertTrue(auction == null);
        }
    }
}

package com.nhom3.server.dao;

import java.util.List;

import com.nhom3.shared.model.item.Item;
import com.nhom3.shared.network.payload.SellerItemsResponsePayload;

public interface ItemDAO {
    boolean saveItem(Item item, int sellerId);
    List<Item> getItemsBySellerId(int sellerId);
    List<SellerItemsResponsePayload.SellerItemDTO> getSellerItemsForManagement(int sellerId);
    String getItemImageBase64(int itemId);
    boolean deleteItem(int itemId, int sellerId);
    boolean updateItem(Item item);
}

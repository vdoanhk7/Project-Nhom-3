package com.nhom3.User;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import com.nhom3.Item.Item;
public class Seller extends User {
    private List<Item> managedItems;

    public Seller(String id, String userName, String password, String name, String email, String phoneNumber) {
        super("S-" + id, userName, password, name, email, phoneNumber);
        this.managedItems = new ArrayList<>();
    }
    // Các phương thức quản lý item
    public void addItem(Item newItem) { // Thêm item mới vào danh sách quản lý
        if (newItem != null) {
            this.managedItems.add(newItem);
        }
    }
    public boolean removeItem(String itemId) {  //Xoá item theo id 
        for (int i = 0; i < managedItems.size(); i++) {
            if (managedItems.get(i).getId().equals(itemId)) {
                managedItems.remove(i);
                return true; // Xóa thành công
            }
        }
        return false; // Không tìm thấy item với id đã cho
    }
    public boolean updateItem(String itemId, String newName, Double newStartPrice, LocalDate newStartAution, LocalDate newEndAuction) { // Cập nhật thông tin item theo id
        for (Item item : managedItems) {
            if (item.getId().equals(itemId)) {
                item.setName(newName);
                item.setStartPrice(newStartPrice);
                item.setStartAuction(newStartAution);
                item.setEndAuction(newEndAuction);
                return true; // Cập nhật thành công
            }
        }
        return false; // Không tìm thấy item với id đã cho
    }

}



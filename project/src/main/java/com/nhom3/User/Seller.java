package com.nhom3.User;
import java.util.ArrayList;
import java.util.List;
import com.nhom3.Item.Item;
public class Seller extends User {
    private List<Item> managedItems;

    public Seller(String id, String userName, String password, String name, String email, String phoneNumber) {
        super("S-" + id, userName, password, name, email, phoneNumber);
        this.managedItems = new ArrayList<>();
    }
    @Override
    public String getRoleName() {
        return "SELLER";
    }
    // Các phương thức quản lý item
    public void addItem(Item newItem) { // Thêm item mới vào danh sách quản lý
        if (newItem != null) {
            this.managedItems.add(newItem);
        }
    }
    public boolean removeItem(String itemId) {  //
        for (int i = 0; i < managedItems.size(); i++) {
            if (managedItems.get(i).getId().equals(itemId)) {
                managedItems.remove(i);
                return true // Xóa thành công
            }
        }
        return false; // Không tìm thấy item với id đã cho
    }
    public void updateItem() {
        }

}



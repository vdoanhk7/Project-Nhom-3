package com.nhom3.shared.network.payload;
import java.util.List;

public class SellerItemsResponsePayload {
    
    // Class DTO (Data Transfer Object) chỉ chứa dữ liệu thô để tránh lỗi Gson
    public static class SellerItemDTO {
        public int id;
        public String name;
        public String type;
        public double startPrice;
        public double curHighest;
        public String status; 

        public SellerItemDTO(int id, String name, String type, double startPrice, double curHighest, String status) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.startPrice = startPrice;
            this.curHighest = curHighest;
            this.status = status;
        }
    }

    private List<SellerItemDTO> items;

    public SellerItemsResponsePayload(List<SellerItemDTO> items) {
        this.items = items;
    }
    public List<SellerItemDTO> getItems() { return items; }
}
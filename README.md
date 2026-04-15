# Project-Nhom-3
Phuc Anh: Auto-Bidding
Doanh:Gia hạn phiên đấu giá
Dung: Bid History Visualization: biểu đồ đường giá realtime
Quang Anh: tinh nang phu
Quang Anh: JavaFx
network: Doanh + Phuc Anh
Xu ly loi: Lam chung
Realtime update (Observer/Socket):Phuc Anh
Unit Test, CI/CD: Dung
Refactor: Doanh
Tuan 7: Hieu code

src/main/java/com/nhom3/
├── client/                     # Phía Client (Giao diện JavaFX)
│   ├── App.java
│   ├── Main.java
│   ├── controller/             # Xử lý sự kiện giao diện
│   │   ├── LoginController.java
│   │   └── ...
+   └── network/                # Xử lý giao tiếp với Server (Gửi/Nhận dữ liệu)
+       └── SocketClient.java   # (Hoặc RestClient nếu dùng REST)
│
├── server/                     # Phía Server (Xử lý nghiệp vụ & Database)
+   ├── ServerApp.java          # File Main để chạy Server
+   ├── controller/             # Nhận request từ Client, gọi Service
+   │   └── AuctionController.java
│   ├── service/                # Chứa logic nghiệp vụ (đã có AuctionService)
│   │   └── AuctionService.java 
+   ├── dao/                    # (Data Access Object) - Truy vấn Database
+   │   ├── UserDAO.java
+   │   └── ItemDAO.java
+   ├── database/               # Quản lý kết nối DB (Áp dụng Singleton ở đây)
+   │   └── DatabaseConnection.java
+   └── network/                # Lắng nghe kết nối từ Client
+       ├── SocketServer.java
+       └── ClientHandler.java  # Xử lý từng client riêng biệt (Thread)
│
└── shared/                     # Dùng chung cho cả Client và Server
    ├── factory/                # (Đã làm tốt)
    ├── model/                  # (Đã làm tốt, nhớ đổi tên package thành chữ thường)
    │   ├── auction/
    │   ├── item/
    │   └── user/
+   └── network/                # Các class dùng để đóng gói dữ liệu gửi qua mạng
+       ├── Request.java        # VD: Request("LOGIN", user, pass)
+       └── Response.java       # VD: Response("SUCCESS", "Đăng nhập thành công")
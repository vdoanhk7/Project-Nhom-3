# Hệ Thống Đấu Giá Trực Tuyến - Nhóm 3

## 1. Mô Tả Bài Toán Và Phạm Vi Hệ Thống

Hệ thống đấu giá trực tuyến cho phép người bán đăng sản phẩm, tạo phiên đấu giá và người mua tham gia đặt giá theo thời gian thực. Hệ thống được xây dựng theo mô hình Client/Server, trong đó server xử lý nghiệp vụ, quản lý dữ liệu và đồng bộ trạng thái đấu giá; client là ứng dụng JavaFX để người dùng thao tác trực tiếp.

Phạm vi thực hiện:

- Server TCP xử lý nhiều client đồng thời.
- Client JavaFX cho các vai trò Bidder, Seller và Admin.
- Quản lý tài khoản, đăng ký, đăng nhập, đổi mật khẩu và cập nhật hồ sơ.
- Quản lý sản phẩm, phiên đấu giá, đặt giá, auto bid, lịch sử đấu giá và lịch sử mua hàng.
- Cập nhật realtime cho các phiên đấu giá đang diễn ra.
- Ghi log hệ thống và hỗ trợ màn hình quản trị.
- Sử dụng MySQL để lưu trữ người dùng, sản phẩm, phiên đấu giá, lịch sử đặt giá và lịch sử mua hàng.


## 2. Công Nghệ Sử Dụng, Môi Trường Chạy Và Yêu Cầu Cài Đặt

Công nghệ chính:

- Java 21: ngôn ngữ lập trình chính, sử dụng virtual threads để xử lý nhiều client đồng thời ở phía server.
- Maven: quản lý dependencies, build project và tạo fat JAR bằng `maven-shade-plugin`.
- JavaFX 21: xây dựng giao diện desktop cho client.
- MySQL: lưu trữ dữ liệu người dùng, sản phẩm, phiên đấu giá, lịch sử đặt giá và lịch sử mua hàng.
- Socket TCP: giao tiếp mạng giữa client và server.
- Gson: chuyển đổi dữ liệu giữa object Java và JSON khi gửi/nhận packet qua socket.
- HikariCP: quản lý connection pool tới MySQL.
- SLF4J và Logback: ghi log hoạt động của server, client và hệ thống.
- jBCrypt: mã hóa và kiểm tra mật khẩu người dùng.
- JUnit 5, Mockito: viết unit test và kiểm thử các service, DAO, model.
- H2 Database: database in-memory dùng cho test.
- JaCoCo: đo test coverage.
- Checkstyle: kiểm tra style code.
- SpotBugs: phân tích lỗi tiềm ẩn trong code.
- GitHub Actions: tự động kiểm tra build/test và build artifact fat JAR.


Môi trường yêu cầu:

- JDK 21
- Maven 3.8+ hoặc bản mới hơn
- Có database MySQL để server kết nối tới, có thể là MySQL cài trên máy hoặc MySQL cloud
- Hệ điều hành Windows, Linux hoặc macOS

Nếu không dùng database cloud mặc định trong code, cần chuẩn bị database MySQL riêng:

1. Tạo database MySQL.
2. Import cấu trúc bảng từ file `project/database/schema.sql`.
3. Nếu cần dữ liệu mẫu để demo, import thêm `project/database/mock_data.sql`.

Ví dụ khởi tạo database:

```bash
mysql -u <user> -p -e "CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u <user> -p auction_db < project/database/schema.sql
mysql -u <user> -p auction_db < project/database/mock_data.sql
```

## 3. Cấu Trúc Thư Mục Và Các Module Chính

```text
Project-Nhom-3/
|-- .github/workflows/
|   |-- ci.yml                 # Kiểm tra build/test với Maven
|   `-- package.yml            # Build và upload server/client fat JAR
|-- project/
|   |-- database/
|   |   |-- schema.sql         # Cấu trúc database
|   |   `-- mock_data.sql      # Dữ liệu mẫu
|   |-- dist/                  # Nơi sinh ra file JAR sau khi build
|   |-- src/main/java/com/nhom3/
|   |   |-- client/            # Ứng dụng JavaFX client
|   |   |-- server/            # Server, DAO, service, network handler
|   |   `-- shared/            # Model, packet, payload dùng chung
|   |-- src/main/resources/    # FXML, ảnh, cấu hình logback
|   |-- src/test/java/         # Unit test và integration test
|   `-- pom.xml                # Cấu hình Maven và fat JAR
`-- README.md
```

## 4. Vị Trí Các File JAR

Sau khi build, Maven tạo 2 fat JAR tại(Khi clone code thì đã có file JAR sẵn để chạy:

```text
project/dist/server-app.jar
project/dist/client-app.jar
```

Hai file này là fat JAR/uber JAR, đã đóng gói các dependencies cần thiết để chạy bằng lệnh `java -jar`.

GitHub Actions workflow `Build Fat JARs` cũng tự động build và upload artifact tên `auction-fat-jars`, gồm:

```text
server-app.jar
client-app.jar
```

## 5. Hướng Dẫn Build

Từ thư mục gốc repository, build nhanh và bỏ qua test:

```bash
cd project
mvn --batch-mode -DskipTests clean package
```

Nếu muốn build kèm test:

```bash
cd project
mvn --batch-mode clean package
```

## 6. Hướng Dẫn Chạy Server/Client

Cần chuẩn bị database MySQL trước, sau đó chạy server, rồi mới chạy client.

Chạy nhanh với cấu hình mặc định:

Bước 1: vào project và chạy server

```bash
cd Project-Nhom-3/project
java -jar dist/server-app.jar
```

Bước 2: chạy client

```bash
cd Project-Nhom-3/project
java -jar dist/client-app.jar
```

Để chạy với cấu hình khác, tham khảo mục 6.1.

### 6.1. Cấu Hình Có Thể Truyền Khi Chạy

`server-app.jar`:

| Mục cấu hình | Java system property | Environment variable | Mặc định |
|---|---|---|---|
| Port server lắng nghe | `auction.server.port` | `AUCTION_SERVER_PORT` | `8080` |
| URL database | `auction.db.url` | `AUCTION_DB_URL` | Database cloud trong code |
| Username database | `auction.db.user` | `AUCTION_DB_USER` | Username cloud trong code |
| Password database | `auction.db.password` | `AUCTION_DB_PASSWORD` | Password cloud trong code |
| Driver database | `auction.db.driver` | `AUCTION_DB_DRIVER` | `com.mysql.cj.jdbc.Driver` |
| Số connection tối đa | `auction.db.pool.max` | `AUCTION_DB_POOL_MAX` | `20` |
| Số connection idle tối thiểu | `auction.db.pool.min` | `AUCTION_DB_POOL_MIN` | `5` |

`client-app.jar`:

| Mục cấu hình | Java system property | Environment variable | Mặc định |
|---|---|---|---|
| Địa chỉ server | `auction.server.host` | `AUCTION_SERVER_HOST` | `localhost` |
| Port server | `auction.server.port` | `AUCTION_SERVER_PORT` | `8080` |

### 6.2. Chạy Server

Muốn thay đổi thông tin cấu hình, dùng Java system properties hoặc cấu hình sẵn Environment variable:

Ví dụ muốn truy cập server đã chạy trên máy ảo tại id "34.126.166.158"

```bash
java -Dauction.server.host=34.126.166.158 -jar dist/server-app.jar
```

### 6.3. Chạy Client

Nếu server chạy trên máy khác hoặc port khác, truyền cấu hình server cho client bằng Java system properties/Environment variable:

Ví dụ bằng Java system properties:  

```bash
java -Dauction.server.host=<server-host> -Dauction.server.port=<server-port> -jar dist/client-app.jar
```

### 6.4. Chạy Nhiều Client

Mở terminal khác và chạy client

## 7. Danh Sách Chức Năng Đã Hoàn Thành

- Đăng ký tài khoản với vai trò Bidder hoặc Seller.
- Đăng ký có validate email, số điện thoại, mật khẩu và xác nhận mật khẩu. 
- Đăng nhập, đăng xuất và phân quyền giao diện theo vai trò Bidder, Seller, Admin.
- Đổi mật khẩu và bảo mật mật khẩu người dùng.
- Xem, chỉnh sửa hồ sơ cá nhân và cập nhật ảnh đại diện.
- Tự thông báo cho người dùng khi tài khoản đang đăng nhập bị xóa bởi Admin.

- Hiển thị dashboard tổng quan với thống kê hệ thống, top bidder và top sản phẩm.
- Điều hướng nhanh từ dashboard tới các màn hình chính theo vai trò người dùng.

- Bidder xem chợ đấu giá và danh sách các phiên đang/sắp diễn ra.
- Bidder tìm kiếm, lọc phiên đấu giá theo tên sản phẩm, người bán, danh mục và thời gian còn lại.
- Bidder xem chi tiết sản phẩm, thông tin phiên đấu giá và trạng thái phiên.
- Bidder đặt giá trực tiếp trong phiên đấu giá.
- Bidder xem lịch sử đặt giá của từng phiên đấu giá.
- Bidder xem biểu đồ biến động giá trong phiên đấu giá.
- Bidder thiết lập, thay đổi, kiểm tra trạng thái và hủy auto bid.
- Bidder xem lịch sử đấu giá cá nhân.
- Bidder tìm kiếm và lọc lịch sử đấu giá theo kết quả tham gia.

- Seller thêm, sửa, xóa và quản lý sản phẩm.
- Seller upload, xem trước và hiển thị ảnh sản phẩm.
- Seller quản lý sản phẩm theo mã sản phẩm, tên sản phẩm và danh mục.
- Seller đăng bán sản phẩm ngay hoặc hẹn giờ mở phiên đấu giá.
- Seller cấu hình thời gian bắt đầu, thời gian kết thúc và bước giá cho phiên đấu giá.
- Seller xem chi tiết phiên đấu giá gắn với sản phẩm.
- Seller xác nhận thanh toán sau khi phiên đấu giá kết thúc.

- Admin xem thống kê tổng quan hệ thống.
- Admin xem danh sách người dùng và thông tin chi tiết người dùng.
- Admin xóa tài khoản người dùng.
- Admin xem danh sách tất cả phiên đấu giá.
- Admin lọc phiên đấu giá theo trạng thái.
- Admin hủy phiên đấu giá khi cần.
- Admin theo dõi log hệ thống trên màn hình quản trị.

- Đề xuất hiển thị phiên đấu giá cho Bidder theo mức độ tương tác
- Điểm uy tín cho Bidder và star cho Seller 
- Tự động chuyển trạng thái phiên đấu giá theo thời gian đã cấu hình.
- Tự động mở phiên khi đến giờ bắt đầu và đóng phiên khi hết hạn.
- Hỗ trợ chống đặt giá vào phút cuối (anti-sniping).
- Cập nhật realtime giá hiện tại cho các client đang theo dõi phiên đấu giá.
- Cập nhật realtime khi phiên đấu giá bị hủy.
- Cập nhật realtime log hệ thống cho Admin.
- Rule-based chatbot đơn giản4
- Server hỗ trợ nhiều client kết nối đồng thời.
- Lưu trữ dữ liệu người dùng, sản phẩm, phiên đấu giá, lịch sử đặt giá và auto bid.
- Ghi log hoạt động của server, client và hệ thống.
- Có unit test/integration test cho các lớp service, DAO và model chính.
- Hỗ trợ tự động kiểm tra build/test và tạo artifact chạy ứng dụng.

## 8. Báo Cáo PDF Và Video Demo

- Báo cáo PDF: TODO - cập nhật link báo cáo PDF
- Video demo: TODO - cập nhật link video demo

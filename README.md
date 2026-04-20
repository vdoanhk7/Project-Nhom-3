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

<pre>
## 🏗️ Kiến trúc Hệ thống (Architecture)
Dự án được xây dựng theo mô hình **Client - Server**, áp dụng các Design Pattern cốt lõi như `Singleton` và `Factory Method`.

```plantuml
@startuml
skinparam classAttributeIconSize 0
skinparam linetype ortho
skinparam packageStyle rectangle

title Tổng quan Kiến trúc Hệ thống Đấu giá (Project Nhóm 3)

' ==========================================
' 1. SHARED (MODELS & PATTERNS)
' ==========================================
package "com.nhom3.shared" <<Folder>> {
    
    package "model" {
        abstract class User {
            # role: Role
        }
        class Bidder extends User
        class Seller extends User
        
        abstract class Item {
            # name: String
            # startPrice: double
            # curHighest: double
        }
        
        class Auction {
            - startTime: LocalDateTime
            - endTime: LocalDateTime
            - status: StatusOfAuction
        }

        class BidTransaction {
            - amount: double
            - time: LocalDateTime
        }

        Seller "1" o--> "*" Item : manages
        Seller "1" o--> "*" Auction : manages
        Auction "1" o--> "1" Item : item
        Auction "1" *--> "*" BidTransaction : bidHistory
        BidTransaction "*" o--> "1" Bidder : bidder
    }

    package "factory (Design Pattern)" {
        interface ItemCreator <<Factory>> {
            + createItem(): Item
        }
        class ArtCreator implements ItemCreator
        class ElectronicsCreator implements ItemCreator
        
        ItemCreator ..> Item : creates
    }
}

' ==========================================
' 2. SERVER (DATABASE & LOGIC)
' ==========================================
package "com.nhom3.server" <<Node>> {
    
    class DbConnection <<Singleton>> {
        + static getInstance(): Connection
    }

    package "dao" {
        interface AuctionDAO {
            + getActiveAuctions()
            + updateHighestBid()
            + closeExpiredAuctions()
        }
        class AuctionDAOImpl implements AuctionDAO
        AuctionDAOImpl ..> DbConnection : queries
    }

    package "service" {
        class AuctionMonitorService <<Background Task>> {
            + startMonitoring()
            - closeExpiredAuctions()
        }
        class AuthService {
            + login()
            + register()
        }
        AuctionMonitorService --> AuctionDAO
    }
}

' ==========================================
' 3. CLIENT (UI & STATE)
' ==========================================
package "com.nhom3.client" <<Node>> {
    
    class UserSession <<Singleton>> {
        - loggedInUser: User
        + static getInstance()
    }

    package "controllers" {
        class MarketController {
            + loadMarket()
            + filterMarket()
        }
        class ViewItemDetailController {
            + handlePlaceBid()
            - startPollingData()
        }
        class ManageItemController
        class PurchaseHistoryController
    }

    MarketController ..> ViewItemDetailController : "Opens Detail"
    ViewItemDetailController ..> AuctionDAO : "Place Bid / Polling"
    MarketController ..> AuctionDAO : "Fetch Market"
    ManageItemController ..> ItemCreator : "Uses Factory"
}

' ==========================================
' 4. NETWORK (Planned)
' ==========================================
package "com.nhom3.network (Dự kiến / Task Tuần tới)" <<Cloud>> {
    class SocketServer
    class SocketClient
    class Request
    class Response
    
    note bottom of SocketServer
        Thay thế việc Client gọi
        trực tiếp DAO (như hiện tại)
        bằng luồng Gửi/Nhận Socket.
    end note
}

@enduml
```
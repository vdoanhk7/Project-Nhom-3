# Dong goi ung dung de gui nguoi dung

Ung dung nay dung JavaFX, nen khong nen gui mot file JAR duy nhat cho ca Windows, macOS va Linux. JavaFX co thu vien native rieng cho tung he dieu hanh. Cach on dinh la tao goi rieng cho tung nen tang bang `jpackage`; goi tao ra da kem runtime Java, nguoi dung khong can cai Java.

## Windows

Chay tren may Windows co JDK 21 va Maven:

```powershell
.\packaging\package-windows.ps1 -ServerHost 34.126.166.158 -ServerPort 8080
```

Hoac:

```cmd
packaging\package-windows.cmd 34.126.166.158 8080
```

File tao ra:

- `release/windows/AuctionClientNhom3-windows.zip`
- `release/windows/AuctionServerNhom3-windows.zip`

Neu muon tao installer `.exe` hoac `.msi`, cai WiX Toolset truoc, roi chay:

```powershell
.\packaging\package-windows.ps1 -Type exe
.\packaging\package-windows.ps1 -Type msi
```

## macOS

Chay tren may macOS co JDK 21 va Maven:

```bash
./packaging/package-macos.sh 34.126.166.158 8080
```

File tao ra:

- `release/macos/AuctionClientNhom3-macos.zip`
- `release/macos/AuctionServerNhom3-macos.zip`

## Linux

Chay tren may Linux co JDK 21 va Maven:

```bash
./packaging/package-linux.sh 34.126.166.158 8080 deb
```

Kieu goi ho tro: `deb`, `rpm`, `app-image`.

Neu dung `app-image`, file tao ra:

- `release/linux/AuctionClientNhom3-linux.tar.gz`
- `release/linux/AuctionServerNhom3-linux.tar.gz`

Neu dung `deb` hoac `rpm`, file tao ra nam trong:

- `release/linux/client`
- `release/linux/server`

## GitHub Actions

Co the vao tab Actions, chay workflow `Package Apps`, nhap `server_host` va `server_port`. Workflow nay build tren 3 runner rieng va upload artifact cho Windows, macOS, Linux.

## Luu y khi gui cho nguoi dung

- Neu server chay tren may/cloud rieng, chi can gui `AuctionClientNhom3-*` cho nguoi dung.
- Neu muon nguoi dung tu chay server, gui them `AuctionServerNhom3-*`.
- Thay `34.126.166.158` bang IP/domain server that te truoc khi dong goi client.

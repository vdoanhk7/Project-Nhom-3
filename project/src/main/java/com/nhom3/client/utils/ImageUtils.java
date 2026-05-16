package com.nhom3.client.utils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

public class ImageUtils {

    // Hàm đọc file, ép kích thước tối đa 500x500, nén thành JPG và trả về chuỗi Base64 siêu nhẹ
    public static String compressAndEncodeImage(File file) throws Exception {
        BufferedImage originalImage = ImageIO.read(file);
        if (originalImage == null) return null;

        int maxWidth = 500;
        int maxHeight = 500;
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        // Tính toán tỷ lệ thu nhỏ nếu ảnh lớn hơn 500px
        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            double widthRatio = (double) maxWidth / originalWidth;
            double heightRatio = (double) maxHeight / originalHeight;
            double ratio = Math.min(widthRatio, heightRatio);

            newWidth = (int) (originalWidth * ratio);
            newHeight = (int) (originalHeight * ratio);
        }

        // Tạo khung ảnh mới (loại bỏ kênh trong suốt Alpha nếu là PNG để ép xuống JPG)
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, java.awt.Color.WHITE, null);
        g.dispose();

        // Ghi ảnh đã resize ra luồng bộ nhớ dạng JPG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);

        // Chuyển mảng byte nén thành Base64
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    private final MessageType type;
    private final String sender;
    private final String content;
    private final long timestamp; // Lưu thời gian dưới dạng số long (mili-giây)

    // Constructor khi TẠO MỚI tin nhắn (Tự động lấy giờ hiện tại)
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis(); 
    }

    // Constructor dùng khi KHÔI PHỤC tin nhắn cũ từ chuỗi thô Socket hoặc DB
    public Message(MessageType type, String sender, String content, long timestamp) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Cập nhật cấu trúc gửi qua mạng: THÊM TIMESTAMP VÀO ĐẦU HOẶC CUỐI GÓI TIN
    public String toRawString() {
        return this.type.name() + "|" + this.sender + "|" + this.timestamp + "|" + this.content;
    }

    // Cập nhật hàm bóc tách dữ liệu từ Socket
    public static Message fromRawString(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            throw new IllegalArgumentException("Dữ liệu trống");
        }
        // split("\\|", 4) tách làm 4 phần: TYPE, SENDER, TIMESTAMP, CONTENT
        String[] tokens = rawData.split("\\|", 4);
        
        MessageType type = MessageType.valueOf(tokens[0]);
        String sender = tokens[1];
        long timestamp = Long.parseLong(tokens[2]);
        String content = tokens[3];
        
        return new Message(type, sender, content, timestamp);
    }

    // Hàm tiện ích giúp biến số long thành chuỗi Giờ:Phút hiển thị trên UI
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date(this.timestamp));
    }

    // Getters
    public MessageType getType() { return type; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}

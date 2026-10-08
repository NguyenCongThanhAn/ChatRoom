/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {

    private final MessageType type;
    private final String sender;
    private final String content;
    private final long timestamp;

    // Constructor khi TẠO MỚI tin nhắn (Tự động lấy giờ hiện tại)
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor dùng khi KHÔI PHỤC tin nhắn từ byte[] hoặc Database
    public Message(MessageType type, String sender, String content, long timestamp) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * CHUYỂN ĐỔI ĐỐI TƯỢNG THÀNH MẢNG BYTE ĐỂ GỬI QUA MULTICAST SOCKET Cấu trúc
     * gói tin: [1 byte Type] + [String Sender] + [8 bytes Timestamp] + [String
     * Content]
     */
    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        // 1. Ghi enum MessageType dưới dạng 1 byte duy nhất (Sử dụng số thứ tự ordinal)
        dos.writeByte(this.type.ordinal());

        // 2. Ghi tên người gửi (Dạng chuỗi mã hóa UTF)
        dos.writeUTF(this.sender);

        // 3. Ghi thời gian dạng long (Tốn cố định 8 bytes)
        dos.writeLong(this.timestamp);

        // 4. Ghi nội dung tin nhắn (Hỗ trợ tiếng Việt đầy đủ)
        dos.writeUTF(this.content);

        dos.flush();
        return baos.toByteArray();
    }

    /**
     * GIẢI MÃ MẢNG BYTE NHẬN ĐƯỢC TỪ SOCKET THÀNH ĐỐI TƯỢNG MESSAGE CHUẨN
     */
    public static Message fromBytes(byte[] data, int length) throws IOException {
        if (data == null || length == 0) {
            throw new IllegalArgumentException("Dữ liệu byte trống");
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, length);
        DataInputStream dis = new DataInputStream(bais);

        try {
            // 1. Đọc 1 byte ra số nguyên và ánh xạ ngược lại về Enum tương ứng
            int typeOrdinal = dis.readByte();
            MessageType type = MessageType.values()[typeOrdinal];

            // 2. Đọc các trường tiếp theo theo đúng tuần tự đã ghi
            String sender = dis.readUTF();
            long timestamp = dis.readLong();
            String content = dis.readUTF();

            return new Message(type, sender, content, timestamp);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Lỗi giải mã: Chỉ mục MessageType không hợp lệ. Có thể do lệch phiên bản mã nguồn giữa các Client.", e);
        } catch (Exception e) {
            throw new IOException("Lỗi bóc tách cấu trúc gói tin byte: " + e.getMessage(), e);
        }
    }

    /**
     * Hàm tiện ích định dạng lại thời gian để hiển thị lên giao diện chat (UI)
     */
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date(this.timestamp));
    }

    // Getters đầy đủ
    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

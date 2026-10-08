package dev.ansida.chat.frontend;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class MulticastManager {
    // Địa chỉ IP nhóm Multicast (Dải Class D: 224.0.0.0 đến 239.255.255.255)
    private static final String GROUP_IP = "225.4.5.6";
    private static final int PORT = 5007;

    private MulticastSocket socket;
    private InetAddress group;
    private boolean isRunning = false;

    // 1. Hàm khởi tạo socket, tham gia nhóm và bắt đầu lắng nghe tin nhắn
    public void start(Consumer<String> onMessageReceived) {
        try {
            // Lắng nghe cổng PORT
            socket = new MulticastSocket(PORT);
            group = InetAddress.getByName(GROUP_IP);
            
            // Đăng ký tham gia nhóm Multicast với Hệ điều hành
            socket.joinGroup(group);
            isRunning = true;

            // Chạy luồng ngầm liên tục nhận gói tin (không gây đơ giao diện)
            new Thread(() -> {
                byte[] buffer = new byte[1024]; // Bộ nhớ tạm 1KB cho mỗi gói tin
                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet); // Chờ tin nhắn tới

                        // Chuyển dữ liệu byte nhận được thành chuỗi văn bản UTF-8
                        String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                        // Truyền tin nhắn ra tầng UI
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(message);
                        }
                    } catch (Exception e) {
                        if (!isRunning) break; // Thoát luồng khi gọi ngắt kết nối
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. Hàm phát tin nhắn tới tất cả thành viên trong nhóm Multicast
    public void sendMessage(String message) {
        try {
            if (socket != null && !socket.isClosed()) {
                byte[] data = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. Hàm rời nhóm và giải phóng cổng kết nối (gọi khi đóng ứng dụng)
    public void stop() {
        try {
            isRunning = false;
            if (socket != null && !socket.isClosed()) {
                socket.leaveGroup(group); // Rời khỏi nhóm
                socket.close();          // Đóng Socket
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
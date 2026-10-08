/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.backend;

/**
 *
 * @author PC
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class MulticastEngine {
    private final int port; // Cổng
    private MulticastSocket socket; // Socket
    private Thread receiveThread; // Thead nhận tin nhắn
    private boolean isRunning; // status
    private NetworkInterface netIf; // Card

    public interface OnPacketReceivedListener {
        void onPacketReceived(String fromGroupIP, byte[] rawData, int length);
    }
    private OnPacketReceivedListener packetListener;

    public MulticastEngine(int port) {
        this.port = port;
        this.isRunning = false;
        initNetworkInterface();
    }

    private void initNetworkInterface() {
        try {
            this.netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (this.netIf == null || !this.netIf.supportsMulticast()) {
                // Nếu lỗi, bốc đại card mạng đầu tiên có sẵn trên hệ điều hành
                this.netIf = NetworkInterface.getNetworkInterfaces().nextElement();
            }
        } catch (Exception e) {
            System.err.println("[Engine Error] Không tìm thấy Card mạng phù hợp: " + e.getMessage());
        }
    }

    // 2. KÍCH HOẠT ENGINE & CHẠY LUỒNG NHẬN TIN NGẦM
    public void start() throws IOException {
        if (isRunning) return;
        
        this.socket = new MulticastSocket(port);
        
        // BẪY UDP 1: Tắt cơ chế Loopback Echo (Dội âm tin nhắn)
        // Trong Java: setLoopbackMode(true) có nghĩa là DISABLE (Vô hiệu hóa) việc tự nhận lại gói tin mình gửi đi
        socket.setLoopbackMode(true); 
        
        this.isRunning = true;

        // Tạo luồng nhận tin nhắn độc lập chạy song song
        this.receiveThread = new Thread(() -> {
            // BẪY UDP 2: Đặt kích thước buffer tối đa của gói tin UDP (64KB) để tránh mất mát dữ liệu
            byte[] buffer = new byte[65507]; 
            
            while (isRunning && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    // Hàm block máy chờ gói tin UDP truyền tới IP Multicast mà socket đang join
                    socket.receive(packet); 

                    String fromGroupIP = packet.getAddress().getHostAddress();
                    if (packetListener != null) {
                        // Bắn mảng byte và độ dài thực tế của gói tin lên cho Controller
                        packetListener.onPacketReceived(fromGroupIP, packet.getData(), packet.getLength());
                    }
                } catch (IOException e) {
                    if (!isRunning) break; // Nếu chủ động tắt engine thì thoát vòng lặp mượt mà
                    System.err.println("[Engine Error] Lỗi trong luồng lắng nghe: " + e.getMessage());
                }
            }
        });
        this.receiveThread.setDaemon(true); // Đảm bảo thread tự tắt khi tắt app chính
        this.receiveThread.start();
        System.out.println("[Engine] Đã kích hoạt Multicast Engine trên Port " + port);
    }

    // 3. GIA NHẬP PHÒNG CHAT (JOIN GROUP)
    public void joinGroup(String ipAddress) throws IOException {
        if (socket == null || socket.isClosed()) return;
        InetAddress group = InetAddress.getByName(ipAddress);
        InetSocketAddress socketAddress = new InetSocketAddress(group, port);
        
        // Gửi lệnh IGMP Join Group ra card mạng
        socket.joinGroup(socketAddress, netIf);
        System.out.println("[Engine] Đã GIA NHẬP thành công nhóm IP: " + ipAddress);
    }

    // 4. RỜI PHÒNG CHAT (LEAVE GROUP)
    public void leaveGroup(String ipAddress) throws IOException {
        if (socket == null || socket.isClosed()) return;
        InetAddress group = InetAddress.getByName(ipAddress);
        InetSocketAddress socketAddress = new InetSocketAddress(group, port);
        
        // Gửi lệnh IGMP Leave Group ra card mạng
        socket.leaveGroup(socketAddress, netIf);
        System.out.println("[Engine] Đã RỜI khỏi nhóm IP: " + ipAddress);
    }

    // 5. PHÁT GÓI TIN BYTE ĐI (SEND PACKET)
    public void sendPacket(byte[] data, String targetIP) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket chưa khởi tạo hoặc đã bị đóng.");
        }
        InetAddress dest = InetAddress.getByName(targetIP);
        DatagramPacket packet = new DatagramPacket(data, data.length, dest, port);
        
        socket.send(packet);
    }

    // Đăng ký nhận sự kiện từ Controller
    public void setOnPacketReceivedListener(OnPacketReceivedListener listener) {
        this.packetListener = listener;
    }

    // 6. ĐÓNG ENGINE & NGẮT KẾT NỐI AN TOÀN
    public void stop() {
        this.isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("[Engine] Đã tắt socket và dừng luồng lắng nghe mạng.");
        }
    }
}


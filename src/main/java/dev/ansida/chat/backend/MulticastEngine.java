package dev.ansida.chat.backend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class MulticastEngine {
    private final int port;
    private MulticastSocket socket;
    private Thread receiveThread;
    private volatile boolean isRunning; // Dùng volatile để đảm bảo an toàn đa luồng khi stop
    private NetworkInterface netIf;

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
        java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            
            // Bỏ qua card mạng đã tắt, card loopback (127.0.0.1) hoặc card không hỗ trợ Multicast
            if (!networkInterface.isUp() || networkInterface.isLoopback() || !networkInterface.supportsMulticast()) {
                continue;
            }
            
            // KIỂM TRA QUAN TRỌNG: Đảm bảo Card mạng này có sở hữu ít nhất một địa chỉ IPv4 thực tế
            java.util.Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof java.net.Inet4Address) { // Đúng card mạng IPv4
                    this.netIf = networkInterface;
                    System.out.println("[Engine] Đã cấu hình đúng Card mạng IPv4: " + networkInterface.getDisplayName());
                    return;
                }
            }
        }
        // Phương án dự phòng cuối cùng nếu không tìm thấy card tối ưu
        this.netIf = NetworkInterface.getNetworkInterfaces().nextElement();
    } catch (Exception e) {
        System.err.println("[Engine Error] Không tìm thấy Card mạng phù hợp: " + e.getMessage());
    }
}


    public void start() throws IOException {
        if (isRunning) return;
        
        this.socket = new MulticastSocket(port);
        // setLoopbackMode(true) nghĩa là VÔ HIỆU HÓA tính năng tự dội lại gói tin của chính mình
        socket.setLoopbackMode(true); 
        this.isRunning = true;

        this.receiveThread = new Thread(() -> {
            while (isRunning && !socket.isClosed()) {
                try {
                    // Tạo buffer mới cho mỗi gói tin để tránh lỗi đè dữ liệu (Data Corruption)
                    byte[] buffer = new byte[65507]; 
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    socket.receive(packet);
                    
                    String fromGroupIP = packet.getAddress().getHostAddress();
                    if (packetListener != null) {
                        // Tạo một mảng vừa đủ bằng kích thước thực tế của gói tin để gửi lên Controller
                        byte[] actualData = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, actualData, 0, packet.getLength());
                        
                        packetListener.onPacketReceived(fromGroupIP, actualData, packet.getLength());
                    }
                } catch (IOException e) {
                    if (!isRunning) break; 
                    System.err.println("[Engine Error] Lỗi trong luồng lắng nghe: " + e.getMessage());
                }
            }
        });
        
        this.receiveThread.setDaemon(true);
        this.receiveThread.start();
        System.out.println("[Engine] Đã kích hoạt Multicast Engine trên Port " + port);
    }

    public void joinGroup(String ipAddress) throws IOException {
        if (socket == null || socket.isClosed()) return;
        InetAddress group = InetAddress.getByName(ipAddress);
        InetSocketAddress socketAddress = new InetSocketAddress(group, port);
        socket.joinGroup(socketAddress, netIf);
        System.out.println("[Engine] Đã GIA NHẬP nhóm IP: " + ipAddress);
    }

    public void leaveGroup(String ipAddress) throws IOException {
        if (socket == null || socket.isClosed()) return;
        InetAddress group = InetAddress.getByName(ipAddress);
        InetSocketAddress socketAddress = new InetSocketAddress(group, port);
        socket.leaveGroup(socketAddress, netIf);
        System.out.println("[Engine] Đã RỜI khỏi nhóm IP: " + ipAddress);
    }

    public void sendPacket(byte[] data, String targetIP) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket chưa khởi tạo hoặc đã bị đóng.");
        }
        InetAddress dest = InetAddress.getByName(targetIP);
        DatagramPacket packet = new DatagramPacket(data, data.length, dest, port);
        socket.send(packet);
    }

    public void setOnPacketReceivedListener(OnPacketReceivedListener listener) {
        this.packetListener = listener;
    }

    public void stop() {
        this.isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Đóng socket sẽ kích hoạt IOException giúp ngắt luôn luồng receiveThread
            System.out.println("[Engine] Đã tắt socket và dừng luồng lắng nghe mạng.");
        }
    }
}


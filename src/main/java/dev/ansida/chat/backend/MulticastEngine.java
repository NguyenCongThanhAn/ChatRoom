package dev.ansida.chat.backend;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class MulticastEngine {
    private final int port;
    private MulticastSocket socket;
    private Thread receiveThread;
    private volatile boolean isRunning;
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
            // Cách 1: Thử lấy card mạng đang kết nối internet/mạng chính (rất chính xác)
            try (DatagramSocket testSocket = new DatagramSocket()) {
                testSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                InetAddress localAddress = testSocket.getLocalAddress();
                NetworkInterface primaryIf = NetworkInterface.getByInetAddress(localAddress);
                if (primaryIf != null && primaryIf.isUp() && primaryIf.supportsMulticast()) {
                    this.netIf = primaryIf;
                    System.out.println("[Engine] Đã chọn Card mạng chính: " + netIf.getDisplayName() + " (" + localAddress.getHostAddress() + ")");
                    return;
                }
            } catch (Exception ignored) {
                // Bỏ qua nếu máy đang offline hoàn toàn, chuyển sang Cách 2
            }

            // Cách 2: Quét danh sách và loại bỏ các card mạng ảo phổ biến
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                String name = networkInterface.getDisplayName().toLowerCase();

                if (!networkInterface.isUp() || networkInterface.isLoopback() || !networkInterface.supportsMulticast()) {
                    continue;
                }
                // Bỏ qua card mạng ảo
                if (name.contains("virtual") || name.contains("vmware") || name.contains("vbox") 
                        || name.contains("docker") || name.contains("wsl") || name.contains("hyper-v") || name.contains("zerotier")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        this.netIf = networkInterface;
                        System.out.println("[Engine] Đã chọn Card mạng IPv4: " + networkInterface.getDisplayName());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Engine Error] Lỗi tìm Card mạng: " + e.getMessage());
        }
    }

    public void start() throws IOException {
        if (isRunning) return;

        // Dùng InetSocketAddress để cho phép nhiều cửa sổ trên cùng 1 máy dùng chung Port
        this.socket = new MulticastSocket(null);
        this.socket.setReuseAddress(true);
        this.socket.bind(new InetSocketAddress(port));

        // QUAN TRỌNG: Gắn cố định card mạng cho cả chiều Gửi và Nhận
        if (this.netIf != null) {
            this.socket.setNetworkInterface(this.netIf);
        }

        // Đặt false để CHO PHÉP nhận gói tin loopback (cần thiết khi test 2 app trên 1 máy)
        this.socket.setLoopbackMode(false);
        this.socket.setTimeToLive(1); // Giới hạn gói tin trong mạng LAN nội bộ

        this.isRunning = true;

        this.receiveThread = new Thread(() -> {
            while (isRunning && !socket.isClosed()) {
                try {
                    byte[] buffer = new byte[65507];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String fromIP = packet.getAddress().getHostAddress();
                    if (packetListener != null) {
                        byte[] actualData = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, actualData, 0, packet.getLength());
                        packetListener.onPacketReceived(fromIP, actualData, packet.getLength());
                    }
                } catch (IOException e) {
                    if (!isRunning) break;
                    System.err.println("[Engine Error] Lỗi luồng lắng nghe: " + e.getMessage());
                }
            }
        });

        this.receiveThread.setDaemon(true);
        this.receiveThread.start();
        System.out.println("[Engine] Đã khởi động Multicast Engine trên Port " + port);
    }

    public void joinGroup(String ipAddress) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket chưa được start!");
        }
        InetAddress group = InetAddress.getByName(ipAddress);
        if (!group.isMulticastAddress()) {
            throw new IllegalArgumentException("Địa chỉ IP không thuộc dải Multicast (224.0.0.0 - 239.255.255.255): " + ipAddress);
        }
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
            socket.close();
            System.out.println("[Engine] Đã tắt socket.");
        }
    }
}

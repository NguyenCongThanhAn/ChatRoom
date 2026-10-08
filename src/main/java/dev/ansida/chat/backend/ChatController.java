/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.backend;

import dev.ansida.chat.common.Group;
import dev.ansida.chat.common.Message;
import dev.ansida.chat.common.MessageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author PC
 */
public class ChatController {
    private final MulticastEngine networkEngine;
    private final List<Group> groupList;
    private Group currentSelectedGroup;
    private String username;

    /**
     * INTERFACE CALLBACK: Báo cho tầng UI biết khi nào cần cập nhật/vẽ lại giao diện.
     * Khi dùng mô hình này, UI hoàn toàn độc lập và chỉ việc "nghe" theo Controller.
     */
    public interface ChatUpdateListener {
        void onChatHistoryUpdated(List<Message> currentMessages);
        void onGroupListLoaded(List<Group> groups);
        void onError(String errorMessage);
    }
    private ChatUpdateListener uiListener;

    public ChatController(int port, String initialUsername) {
        this.networkEngine = new MulticastEngine(port);
        this.groupList = new ArrayList<>();
        this.currentSelectedGroup = null; // Trạng thái ban đầu: Chưa chọn group nào cả
        this.username = initialUsername;

        // Cấu hình nhận diện và bóc tách dữ liệu từ Network Engine
        setupNetworkCallback();
    }

    // 1. TẦNG XỬ LÝ LOGIC GÓI TIN MẠNG (Đóng vai trò Protocol Parser)
    private void setupNetworkCallback() {
        this.networkEngine.setOnPacketReceivedListener((fromGroupIP, rawData, length) -> {
            // Kiểm tra an toàn: Nếu chưa chọn phòng nào hoặc gói tin từ phòng khác đổ về -> Bỏ qua
            if (currentSelectedGroup == null || !fromGroupIP.equals(currentSelectedGroup.getIpAddress())) {
                return;
            }

            try {
                // Giải mã mảng byte nhị phân thành Object Message bằng hàm tĩnh của bạn
                Message receivedMsg = Message.fromBytes(rawData, length);

                // Ghi trực tiếp vào lịch sử lưu trong Object Group hiện tại
                currentSelectedGroup.addMessage(receivedMsg);

                // Báo hiệu cho giao diện cập nhật ngay lập tức
                if (uiListener != null) {
                    uiListener.onChatHistoryUpdated(currentSelectedGroup.getMessageList());
                }
            } catch (IOException e) {
                System.err.println("[Controller Error] Thất bại khi giải mã gói tin: " + e.getMessage());
            }
        });
    }

    // 2. KHỞI CHẠY HỆ THỐNG BACKEND
    public void start() {
        try {
            // Nạp sẵn danh sách phòng học thuật (Bạn có thể thêm bớt tùy ý)
            groupList.add(new Group("Phòng Tổng Hợp", "224.1.1.1"));
            groupList.add(new Group("Phòng Lập Trình", "224.1.1.2"));
            groupList.add(new Group("Phòng Giải Trí", "224.1.1.3"));

            // Bắn danh sách phòng lên UI để nạp vào GroupSelectionPanel
            if (uiListener != null) {
                uiListener.onGroupListLoaded(groupList);
            }

            // Kích hoạt Engine mạng ngầm
            this.networkEngine.start();
            System.out.println("[Controller] Backend đã sẵn sàng. Đang chờ người dùng chọn phòng...");
        } catch (IOException e) {
            if (uiListener != null) {
                uiListener.onError("Không thể khởi động mạng: " + e.getMessage());
            }
        }
    }

    // 3. LOGIC CHUYỂN PHÒNG CHAT CHUẨN ĐA LUỒNG (Rời cũ -> Vào mới)
    public void handleSwitchGroup(Group targetGroup) {
        if (targetGroup == null || targetGroup == currentSelectedGroup) return;

        try {
            // BƯỚC A: Nếu đang ở phòng cũ, gửi tin thông báo và Rời Nhóm (Leave Group)
            if (currentSelectedGroup != null) {
                // (Tùy chọn học thuật): Phát một gói tin hệ thống báo cho mọi người biết mình rời phòng
                sendSystemNotification(MessageType.LEAVE, username + " đã rời phòng chat.");
                
                // Ngắt kết nối IP Multicast cũ ra khỏi card mạng
                networkEngine.leaveGroup(currentSelectedGroup.getIpAddress());
            }

            // BƯỚC B: Chuyển mạch đối tượng dữ liệu hiện tại
            this.currentSelectedGroup = targetGroup;

            // BƯỚC C: Gia nhập vào nhóm IP mạng mới (Join Group)
            networkEngine.joinGroup(currentSelectedGroup.getIpAddress());

            // BƯỚC D: Phát gói tin thông báo mình vừa kết nối vào phòng mới
            sendSystemNotification(MessageType.JOIN, username + " đã tham gia phòng chat.");

            // BƯỚC E: Đồng bộ hiển thị lại toàn bộ lịch sử tin nhắn đã lưu của Object Group này lên UI
            if (uiListener != null) {
                uiListener.onChatHistoryUpdated(currentSelectedGroup.getMessageList());
            }

        } catch (IOException e) {
            if (uiListener != null) {
                uiListener.onError("Lỗi đường truyền mạng khi đổi phòng: " + e.getMessage());
            }
        }
    }

    // 4. LOGIC GỬI TIN NHẮN VĂN BẢN (CHAT MESSAGE)
    public void sendChatMessage(String content) {
        if (currentSelectedGroup == null || content.trim().isEmpty()) return;

        try {
            // Khởi tạo Object Message theo constructor tạo mới của bạn
            Message chatMsg = new Message(MessageType.CHAT, username, content);
            
            // Ép cấu trúc mảng byte và hạ lệnh bắn qua mạng LAN
            byte[] rawBytes = chatMsg.toBytes();
            networkEngine.sendPacket(rawBytes, currentSelectedGroup.getIpAddress());
            
        } catch (IOException e) {
            if (uiListener != null) {
                uiListener.onError("Không thể gửi tin nhắn chat: " + e.getMessage());
            }
        }
    }

    // Hàm tiện ích phát các thông báo hệ thống (Join / Leave) bằng mảng byte
    private void sendSystemNotification(MessageType type, String content) throws IOException {
        Message sysMsg = new Message(type, "HỆ THỐNG", content);
        byte[] rawBytes = sysMsg.toBytes();
        networkEngine.sendPacket(rawBytes, currentSelectedGroup.getIpAddress());
    }

    // Getters & Setters công khai để UI tương tác
    public void setUsername(String username) {
        this.username = username;
    }

    public Group getCurrentSelectedGroup() {
        return currentSelectedGroup;
    }

    public void setChatUpdateListener(ChatUpdateListener listener) {
        this.uiListener = listener;
    }

    // Tắt hệ thống an toàn khi người dùng bấm thoát app
    public void shutdown() {
        if (currentSelectedGroup != null) {
            try {
                sendSystemNotification(MessageType.LEAVE, username + " đã thoát ứng dụng.");
                networkEngine.leaveGroup(currentSelectedGroup.getIpAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        networkEngine.stop();
    }
}

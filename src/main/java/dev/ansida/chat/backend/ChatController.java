package dev.ansida.chat.backend;

import dev.ansida.chat.common.Group;
import dev.ansida.chat.common.Message;
import dev.ansida.chat.common.MessageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    private final MulticastEngine networkEngine;
    private final List<Group> groupList;
    private Group currentSelectedGroup;
    private String username;

    public interface ChatUpdateListener {

        void onChatHistoryUpdated(List<Message> currentMessages);

        void onGroupListLoaded(List<Group> groups);

        void onError(String errorMessage);
    }
    private ChatUpdateListener uiListener;

    public ChatController(int port, String initialUsername) {
        this.networkEngine = new MulticastEngine(port);
        this.groupList = new ArrayList<>();
        this.currentSelectedGroup = null;
        this.username = initialUsername;

        // Khởi tạo sẵn danh sách phòng chat cố định
        groupList.add(new Group("Phòng Tổng Hợp", "224.1.1.1"));
        groupList.add(new Group("Phòng Lập Trình", "224.1.1.2"));
        groupList.add(new Group("Phòng Giải Trí", "224.1.1.3"));

        setupNetworkCallback();
    }

    // 1. TẦNG XỬ LÝ LOGIC GÓI TIN MẠNG ĐẾN (Từ người khác)
    // 1. TẦNG XỬ LÝ LOGIC GÓI TIN MẠNG (Đóng vai trò Protocol Parser)
    private void setupNetworkCallback() {
        this.networkEngine.setOnPacketReceivedListener((fromGroupIP, rawData, length) -> {
            // YÊU CẦU: Nếu chưa chọn phòng hoặc gói tin đổ về từ IP khác với phòng hiện tại -> Hủy bỏ ngay lập tức
            if (currentSelectedGroup == null || !fromGroupIP.equals(currentSelectedGroup.getIpAddress())) {
                return;
            }

            try {
                // Giải mã mảng byte nhị phân thành Object Message bằng hàm tĩnh ban đầu của bạn
                Message receivedMsg = Message.fromBytes(rawData, length);

                // BẪY UDP MULTICAST: Nếu là tin nhắn do chính máy mình gửi đi (trường hợp card mạng lỗi dội âm) 
                // thì bỏ qua để tránh trùng lặp hiển thị trên giao diện của mình
                if (username.equals(receivedMsg.getSender())) {
                    return;
                }

                // Ghi trực tiếp tin nhắn vào lịch sử lưu trong Object Group hiện tại
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
            // Kích hoạt Engine mạng ngầm trước
            this.networkEngine.start();
            System.out.println("[Controller] Backend Multicast Engine đã chạy.");

            // Bắn danh sách phòng lên UI (Đảm bảo gọi sau khi UI đã setListener)
            if (uiListener != null) {
                uiListener.onGroupListLoaded(groupList);
            }
        } catch (IOException e) {
            if (uiListener != null) {
                uiListener.onError("Không thể khởi động mạng: " + e.getMessage());
            }
        }
    }

    // 3. LOGIC CHUYỂN PHÒNG CHAT (Rời cũ -> Vào mới)
    public void handleSwitchGroup(Group targetGroup) {
        if (targetGroup == null || targetGroup == currentSelectedGroup) {
            return;
        }

        try {
            // Rời phòng cũ nếu có
            if (currentSelectedGroup != null) {
                sendSystemNotification(MessageType.LEAVE, username + " đã rời phòng chat.");
                networkEngine.leaveGroup(currentSelectedGroup.getIpAddress());
            }

            // Chuyển mạch phòng hiện tại
            this.currentSelectedGroup = targetGroup;

            // Gia nhập phòng mới
            networkEngine.joinGroup(currentSelectedGroup.getIpAddress());

            // Thông báo tham gia phòng mới
            sendSystemNotification(MessageType.JOIN, username + " đã tham gia phòng chat.");

            // Đồng bộ lịch sử tin nhắn của phòng mới lên UI
            if (uiListener != null) {
                uiListener.onChatHistoryUpdated(currentSelectedGroup.getMessageList());
            }

        } catch (IOException e) {
            if (uiListener != null) {
                uiListener.onError("Lỗi mạng khi đổi phòng: " + e.getMessage());
            }
        }
    }

    // 4. LOGIC GỬI TIN NHẮN VĂN BẢN
    public void sendChatMessage(String content) {
        if (currentSelectedGroup == null || content.trim().isEmpty()) {
            return;
        }

        try {
            // 1. Khởi tạo tin nhắn text theo đúng constructor ban đầu của bạn (Không có hàm setGroupIpAddress)
            Message chatMsg = new Message(MessageType.CHAT, username, content);

            // 2. Ép cấu trúc mảng byte từ hàm toBytes() nguyên bản
            byte[] rawBytes = chatMsg.toBytes();

            // 3. Phát dữ liệu qua mạng LAN đến đúng địa chỉ IP của phòng đang chọn
            networkEngine.sendPacket(rawBytes, currentSelectedGroup.getIpAddress());

            // 4. Tự cập nhật tin nhắn của chính mình vào bộ nhớ cục bộ (Object Group) và đẩy lên UI
            currentSelectedGroup.addMessage(chatMsg);
            if (uiListener != null) {
                uiListener.onChatHistoryUpdated(currentSelectedGroup.getMessageList());
            }

        } catch (IOException e) {
            if (uiListener != null) {
                uiListener.onError("Không thể gửi tin nhắn chat: " + e.getMessage());
            }
        }
    }

    // Hàm tiện ích phát các thông báo hệ thống (Join / Leave)
    private void sendSystemNotification(MessageType type, String content) throws IOException {
        Message sysMsg = new Message(type, "HỆ THỐNG", content);
        byte[] rawBytes = sysMsg.toBytes();
        networkEngine.sendPacket(rawBytes, currentSelectedGroup.getIpAddress());

        // Tự ghi nhận thông báo hệ thống của chính mình lên UI cục bộ
        currentSelectedGroup.addMessage(sysMsg);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Group getCurrentSelectedGroup() {
        return currentSelectedGroup;
    }

    public void setChatUpdateListener(ChatUpdateListener listener) {
        this.uiListener = listener;
    }

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

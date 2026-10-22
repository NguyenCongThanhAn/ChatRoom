package dev.ansida.chat.frontend;

import dev.ansida.chat.backend.ChatController;
import dev.ansida.chat.common.Group;
import dev.ansida.chat.common.Message;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class ChatUI extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField usernameField;

    // Giữ nguyên 2 thành phần chuyển nhóm hiện tại của bạn bạn
    private JList<Group> groupList; // Ép kiểu Generic thành <Group> để quản lý Object dữ liệu của bạn
    private JButton joinGroupButton;
    private JButton sendButton;
    private MulticastManager network;

    private ChatController controller;

    public ChatUI() {
        setTitle("Ứng Dụng Chat Nhóm Multicast");
        setSize(700, 500); // Tăng chiều rộng lên 700px để chèn thêm JList bên trái cho đẹp
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // KHÔNG khởi tạo chatHistories.put() ở đây nữa vì đã có ChatController và Object Group lo
        // --- 1. TẦNG CẤU HÌNH (TOP PANEL) ---
        JPanel topPanel = new JPanel(new GridLayout(1, 1, 5, 5)); // Đổi thành 1 hàng vì đã bỏ ComboBox

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.add(new JLabel("Tên của bạn:"));
        usernameField = new JTextField("Người dùng " + (int) (Math.random() * 100), 12);
        userPanel.add(usernameField);
        topPanel.add(userPanel);

        add(topPanel, BorderLayout.NORTH);

        // --- 2. TẦNG CHỌN NHÓM (WEST PANEL - THÊM VÀO BÊN TRÁI) ---
        JPanel westPanel = new JPanel(new BorderLayout(5, 5));
        westPanel.setBorder(BorderFactory.createTitledBorder("Danh sách nhóm"));
        westPanel.setPreferredSize(new Dimension(180, 0)); // Chiều rộng vùng danh sách

        // Khởi tạo JList chứa các Object Group
        groupList = new JList<>();
        JScrollPane groupScrollPane = new JScrollPane(groupList);

        joinGroupButton = new JButton("Chuyển Nhóm");

        westPanel.add(groupScrollPane, BorderLayout.CENTER);
        westPanel.add(joinGroupButton, BorderLayout.SOUTH);

        add(westPanel, BorderLayout.WEST); // Đặt panel danh sách nằm bên trái giao diện

        // --- 3. KHUNG HIỂN THỊ TIN NHẮN (CENTER) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // --- 4. Ô NHẬP & NÚT GỬI (BOTTOM PANEL) ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        sendButton = new JButton("Gửi");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 5. KẾT NỐI MẠNG VỚI BACKEND CHAT CONTROLLER CỦA BẠN ---
        initBackendSystem();

        // Sự kiện đổi tên: Tự động cập nhật tên mới vào bộ điều khiển Backend khi click ra ngoài
        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (controller != null) {
                    controller.setUsername(usernameField.getText().trim());
                }
            }
        });

        // Sự kiện khi bấm nút "Chuyển Nhóm"
        joinGroupButton.addActionListener(e -> performSwitchGroup());

        // Sự kiện gửi tin nhắn
        sendButton.addActionListener(e -> performSendMessage());
        inputField.addActionListener(e -> performSendMessage());

        // Bắt sự kiện tắt ứng dụng: Đóng socket và báo Leave phòng an toàn
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (controller != null) {
                    controller.shutdown();
                }
            }
        });
    }

        // 1. HÀM THỰC HIỆN CHUYỂN NHÓM CHAT (ĐÃ SỬA CHUẨN ĐỐI TƯỢNG GROUP)
    private void performSwitchGroup() {
        // Bốc trực tiếp Object Group đang được người dùng click bôi xanh trên JList
        Group selectedGroup = groupList.getSelectedValue();

        if (selectedGroup == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhóm từ danh sách trước!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Nếu người dùng chọn trùng vào phòng hiện tại đang chat thì không cần làm gì
        if (selectedGroup == controller.getCurrentSelectedGroup()) {
            return; 
        }

        // Mở khóa khung nhập liệu cho phép người dùng gõ chữ chat
        inputField.setEnabled(true);
        sendButton.setEnabled(true);

        // Ra lệnh cho Backend thực hiện quy trình mạng: Leave Group cũ -> Join Group mới
        controller.handleSwitchGroup(selectedGroup);
        
        // Trả lại tiêu điểm (Focus) về ô nhập tin nhắn để tiện gõ chữ luôn
        inputField.requestFocus();
    }

    // 2. HÀM THỰC HIỆN GỬI TIN NHẮN (ĐÃ SỬA CHUẨN ĐÓNG GÓI MẢNG BYTE)
    private void performSendMessage() {
        String messageText = inputField.getText().trim();
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!messageText.isEmpty()) {
            // Đồng bộ lại tên người dùng mới nhất phòng trường hợp họ vừa sửa tên trên ô nhập liệu
            controller.setUsername(username);
            
            // Hạ lệnh cho Backend tự động tạo Object Message và mã hóa thành mảng byte phát đi qua UDP
            controller.sendChatMessage(messageText);
            
            inputField.setText(""); // Xóa trống ô gõ chữ sau khi gửi
            inputField.requestFocus();
        }
    }

    // 3. HÀM KHỞI CHẠY CHƯƠNG TRÌNH MẪU (MAIN METHOD)
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            // Khởi tạo cửa sổ giao diện chính và hiển thị lên màn hình
            ChatUI ui = new ChatUI();
            ui.setVisible(true);
        });
    }
private void initBackendSystem() {
        // 1. Khởi tạo bộ điều khiển trên Port 5007 cùng tên người dùng hiện tại
        controller = new ChatController(5007, usernameField.getText().trim());

        // 2. Đăng ký các hành động hiển thị khi mạng có phản hồi (Callback)
        controller.setChatUpdateListener(new ChatController.ChatUpdateListener() {
            @Override
            public void onGroupListLoaded(java.util.List<Group> groups) {
                // Đổ danh sách đối tượng Group sạch từ Backend vào JList của bạn bạn
                DefaultListModel<Group> listModel = new DefaultListModel<>();
                for (Group g : groups) {
                    listModel.addElement(g);
                }
                groupList.setModel(listModel);
                groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                // Khóa khung chat lúc vừa mở app, bắt người dùng chọn nhóm trước
                chatArea.setText("=== VUI LÒNG CHỌN MỘT NHÓM BÊN TRÁI VÀ BẤM 'CHUYỂN NHÓM' ===");
                inputField.setEnabled(false);
                sendButton.setEnabled(false);
            }

            @Override
            public void onChatHistoryUpdated(java.util.List<Message> currentMessages) {
                // Nhận mảng đối tượng Message đã giải mã từ byte mạng để in lên UI
                SwingUtilities.invokeLater(() -> {
                    chatArea.setText(""); // Xóa lịch sử cũ trên màn hình
                    for (Message m : currentMessages) {
                        chatArea.append(String.format("[%s] %s: %s\n", 
                                m.getFormattedTime(), m.getSender(), m.getContent()));
                    }
                    chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Cuộn xuống cuối
                });
            }

            @Override
            public void onError(String errorMessage) {
                JOptionPane.showMessageDialog(ChatUI.this, errorMessage, "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 3. Kích hoạt luồng chạy ngầm Backend bắt đầu mở cổng kết nối
        controller.start();
    }
}

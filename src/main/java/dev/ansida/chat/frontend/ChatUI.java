package dev.ansida.chat.frontend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class ChatUI extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField usernameField;
    private JComboBox<String> groupComboBox;
    private JButton joinGroupButton;
    private JButton sendButton;

    private MulticastManager network;

    // Bộ lưu trữ lịch sử tin nhắn riêng cho từng IP Nhóm
    private Map<String, StringBuilder> chatHistories = new HashMap<>();
    private String currentGroupIp = "225.4.5.6"; // Nhóm mặc định ban đầu

    public ChatUI() {
        setTitle("Ứng Dụng Chat Nhóm Multicast");
        setSize(550, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Khởi tạo lịch sử trống cho các nhóm
        chatHistories.put("225.4.5.6", new StringBuilder());
        chatHistories.put("225.4.5.7", new StringBuilder());
        chatHistories.put("225.4.5.8", new StringBuilder());

        // --- 1. TẦNG CẤU HÌNH (TOP PANEL) ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.add(new JLabel("Tên của bạn:"));
        usernameField = new JTextField("Người dùng " + (int)(Math.random() * 100), 12);
        userPanel.add(usernameField);

        JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        groupPanel.add(new JLabel("Chọn Nhóm:"));

        String[] groups = {
            "225.4.5.6 (Nhóm Chung)", 
            "225.4.5.7 (Nhóm Lập Trình)", 
            "225.4.5.8 (Nhóm Giải Trí)"
        };
        groupComboBox = new JComboBox<>(groups);
        joinGroupButton = new JButton("Chuyển Nhóm");

        groupPanel.add(groupComboBox);
        groupPanel.add(joinGroupButton);

        topPanel.add(userPanel);
        topPanel.add(groupPanel);
        add(topPanel, BorderLayout.NORTH);

        // --- 2. KHUNG HIỂN THỊ TIN NHẮN (CENTER) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // --- 3. Ô NHẬP & NÚT GỬI (BOTTOM PANEL) ---
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        sendButton = new JButton("Gửi");
        
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 4. KẾT NỐI MẠNG MULTICAST ---
        network = new MulticastManager();

        // Lắng nghe tin nhắn tới
        network.start(message -> {
            SwingUtilities.invokeLater(() -> {
                // 1. Lưu tin nhắn vào lịch sử của nhóm hiện tại
                StringBuilder history = chatHistories.computeIfAbsent(currentGroupIp, k -> new StringBuilder());
                history.append(message).append("\n");

                // 2. Cập nhật lên khung chat
                chatArea.setText(history.toString());
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            });
        });

        // Sự kiện đổi nhóm
        joinGroupButton.addActionListener(e -> performSwitchGroup());

        // Sự kiện gửi tin nhắn
        sendButton.addActionListener(e -> performSendMessage());
        inputField.addActionListener(e -> performSendMessage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                network.stop();
            }
        });
    }

    // Hàm thực hiện chuyển nhóm chat
    private void performSwitchGroup() {
        String selectedGroup = (String) groupComboBox.getSelectedItem();
        String newGroupIp = selectedGroup.split(" ")[0]; 

        if (newGroupIp.equals(currentGroupIp)) {
            return; // Nếu bấm vào nhóm đang chat thì không cần làm gì
        }

        // 1. Rời IP nhóm cũ, tham gia IP nhóm mới ở Socket
        network.switchGroup(newGroupIp);

        // 2. Cập nhật IP nhóm hiện tại
        currentGroupIp = newGroupIp;

        // 3. Tải lại đúng lịch sử chat của nhóm mới lên giao diện
        StringBuilder history = chatHistories.computeIfAbsent(currentGroupIp, k -> new StringBuilder());
        chatArea.setText(history.toString());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void performSendMessage() {
        String messageText = inputField.getText().trim();
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!messageText.isEmpty()) {
            String fullFormattedMessage = username + ": " + messageText;
            network.sendMessage(fullFormattedMessage);
            inputField.setText("");
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ChatUI ui = new ChatUI();
            ui.setVisible(true);
        });
    }
}
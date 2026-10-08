/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package dev.ansida.chat.client;

import com.formdev.flatlaf.FlatLightLaf;
import dev.ansida.chat.common.Message;
import dev.ansida.chat.common.MessageType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author PC
 */
public class ChatClient extends javax.swing.JFrame {

    private static final String SERVER_HOST = "0.tcp.ap.ngrok.io";
    private static final int SERVER_PORT = 28164;

    private Socket socket;
    private DataInputStream reader;
    private DataOutputStream writer;
    private String username;

    private ChatClient() throws IOException {
        initComponents();
        username = JOptionPane.showInputDialog(this, "Nhập tên của bạn:", "Đăng nhập", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            username = "User_" + System.currentTimeMillis() % 1000;
        }
            this.setTitle(username);
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            writer = new DataOutputStream(socket.getOutputStream());
            reader = new DataInputStream(socket.getInputStream());

            // 1. Gửi gói tin LOGIN ngay khi kết nối thành công
            // Cấu trúc gói tin thô: TYPE|SENDER|TIMESTAMP|CONTENT
            writer.writeUTF("LOGIN|" + username + "|" + System.currentTimeMillis() + "|");

            // 2. Tự động gửi yêu cầu đòi lấy Chat Log cũ từ Server
            writer.writeUTF("REQ_LOG|" + username + "|" + System.currentTimeMillis() + "|");
            // 3. Tạo luồng chạy ngầm để liên tục nhận dữ liệu từ server mà không làm đơ UI
            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            display.append("SYSTEM: Không thể kết nối tới Server tại " + SERVER_HOST + ":" + SERVER_PORT + "\n");
            intputTf.setEnabled(false);
            sendBt.setEnabled(false);
            attachBt.setEnabled(false);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        intputTf = new javax.swing.JTextField();
        jSplitPane1 = new javax.swing.JSplitPane();
        attachBt = new javax.swing.JButton();
        sendBt = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        display = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        intputTf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intputTfActionPerformed(evt);
            }
        });

        attachBt.setText("Attach");
        attachBt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                attachBtActionPerformed(evt);
            }
        });
        jSplitPane1.setLeftComponent(attachBt);

        sendBt.setText("Send");
        sendBt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendBtActionPerformed(evt);
            }
        });
        jSplitPane1.setRightComponent(sendBt);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(intputTf, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intputTf, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        display.setEditable(false);
        display.setColumns(20);
        display.setRows(5);
        display.setToolTipText("");
        jScrollPane1.setViewportView(display);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void attachBtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_attachBtActionPerformed
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // Giới hạn file nhỏ để tránh tràn bộ nhớ Base64 (< 5MB)
            if (file.length() > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn file dưới 5MB để gửi qua Base64!");
                return;
            }

            // Đọc file và mã hóa sang Base64 chạy ngầm để giao diện không bị giật
            new Thread(() -> {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    String base64Content = Base64.getEncoder().encodeToString(fileBytes);

                    // Nội dung file gồm: Tên_File|Mã_Base64
                    String filePayload = file.getName() + "|" + base64Content;
                    Message message = new Message(MessageType.FILE, username, filePayload);

                    writer.writeUTF(message.toRawString());
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> display.append("SYSTEM: Lỗi đọc file không thành công!\n"));
                }
            }).start();
        }
    }//GEN-LAST:event_attachBtActionPerformed

    private void sendBtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendBtActionPerformed
        sendMessage();
    }//GEN-LAST:event_sendBtActionPerformed

    private void intputTfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intputTfActionPerformed
        sendMessage();
    }//GEN-LAST:event_intputTfActionPerformed

    private void sendMessage() {
        String text = intputTf.getText().trim();
        if (!text.isEmpty()) {
            // Tạo đối tượng Message -> Tự động sinh timestamp bên trong
            Message msg = new Message(MessageType.CHAT, username, text);
            try {
                writer.writeUTF(msg.toRawString()); // "CHAT|Huy|1719234812|Hello"
            } catch (IOException ex) {
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            intputTf.setText("");
        }
    }

    private void handleIncomingFile(String filePayload) {
        try {
            String[] parts = filePayload.split("\\|", 2);
            if (parts.length < 2) {
                return;
            }

            String fileName = parts[0];
            String base64Data = parts[1];

            display.append("SYSTEM: Nhận được file '" + fileName + "'. Đang tải...\n");

            // Hỏi người dùng nơi lưu file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File targetFile = fileChooser.getSelectedFile();
                byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                Files.write(targetFile.toPath(), decodedBytes);
                display.append("SYSTEM: Đã lưu file thành công tại " + targetFile.getAbsolutePath() + "\n");
            }
        } catch (Exception e) {
            display.append("SYSTEM: Lỗi khi xử lý file tải về!\n");
        }
    }

    private void listenToServer() {
        try {
            while (true) {
            String rawData = reader.readUTF();
                // KHÔNG CẦN SPLIT THỦ CÔNG NỮA -> Dùng thẳng class Message dùng chung
                Message incomingMsg = Message.fromRawString(rawData);

                SwingUtilities.invokeLater(() -> {
                    switch (incomingMsg.getType()) {
                        case CHAT:
                            display.append(incomingMsg.getSender() + ": " + incomingMsg.getContent() + "\n");
                            break;
                        case RES_LOG:
                            display.append("[Lịch sử] " + incomingMsg.getSender() + ": " + incomingMsg.getContent() + "\n");
                            break;
                        case SYSTEM:
                            display.append("SYSTEM: " + incomingMsg.getContent() + "\n");
                            break;
                        case FILE:
                            handleIncomingFile(incomingMsg.getContent());
                            break;
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> display.append("SYSTEM: Mất kết nối với Server.\n"));
        }
    }

    private void print(Message mes) {
        SwingUtilities.invokeLater(() -> {
            display.append(mes.getSender() + ": " + mes.getContent() + '\n');
        });
    }

    public static void main(String args[]) {

        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new ChatClient().setVisible(true);
            } catch (IOException ex) {
                Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton attachBt;
    private javax.swing.JTextArea display;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JTextField intputTf;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton sendBt;
    // End of variables declaration//GEN-END:variables
}

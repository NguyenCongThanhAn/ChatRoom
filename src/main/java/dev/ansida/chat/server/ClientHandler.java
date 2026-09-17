/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.server;

import dev.ansida.chat.common.Message;
import dev.ansida.chat.common.MessageType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PC
 */
public class ClientHandler implements Runnable {

    private String id;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public ClientHandler(Socket s) {
        try {
            this.socket = s;
            this.dis = new DataInputStream(s.getInputStream());
            this.dos = new DataOutputStream(s.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String raw = dis.readUTF();
                Message message = Message.fromRawString(raw);
                switch (message.getType()) {
                    case LOGIN -> {
                        this.id = message.getSender();
                        SingleSourceOfTruth.getInstance().addClient(this.id, this);
                    }
                    case REQ_LOG -> {
                        List<Message> history = SingleSourceOfTruth.getInstance().getChatHistory();
                        for (Message oldMsg : history) {
                            // Đóng gói lại thành gói tin phản hồi lịch sử (RES_LOG)
                            Message resLog = new Message(MessageType.RES_LOG, oldMsg.getSender(), oldMsg.getContent());
                            this.dos.writeUTF(resLog.toRawString());
                        }
                    }
                    case CHAT -> {
                        if (true) {
                            SingleSourceOfTruth.getInstance().addMessageToLog(message);

                            // Broadcast chuỗi thô cho tất cả mọi người
                            for (ClientHandler client : SingleSourceOfTruth.getInstance().getAllClients()) {
                                client.dos.writeUTF(message.toRawString());
                                client.dos.flush();
                            }
                        }
                    }

                    case FILE -> {
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();

                        FileOutputStream fos = new FileOutputStream("./downloads/" + fileName);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalRead = 0;

                        while (totalRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }
                        fos.close();
                        System.out.println("Đã nhận xong file: " + fileName);
                    }
                }
            }
        } catch (Exception e) {

        }
    }

}

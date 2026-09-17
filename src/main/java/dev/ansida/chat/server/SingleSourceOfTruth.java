/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.server;

import dev.ansida.chat.common.Message;
import dev.ansida.chat.common.MessageType;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SingleSourceOfTruth {
    
    // Quản lý danh sách Client online (Key: username, Value: ClientHandler)
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    
    // Quản lý lịch sử tin nhắn dạng List đối tượng Message (Thread-safe)
    private final List<Message> chatHistory = new CopyOnWriteArrayList<>();
    
    // Singleton Instance
    private static final SingleSourceOfTruth instance = new SingleSourceOfTruth();

    private SingleSourceOfTruth() {}
    
    public static SingleSourceOfTruth getInstance() {
        return instance;
    }

    // ================= MỤC QUẢN LÝ CLIENT =================
    
    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
    }

    public void removeClient(String username) {
        if (username != null) {
            clients.remove(username);
        }
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public Collection<ClientHandler> getAllClients() {
        return clients.values();
    }

    // ================= MỤC QUẢN LÝ CHAT LOG =================
    
    /**
     * Thêm một đối tượng Message vào lịch sử hệ thống
     */
    public void addMessageToLog(Message message) {
        // Chỉ lưu tin nhắn văn bản (CHAT) hoặc thông báo hệ thống (SYSTEM)
        // Hạn chế lưu FILE trực tiếp vào RAM để tránh tràn bộ nhớ
        if (message.getType() == MessageType.CHAT || message.getType() == MessageType.SYSTEM) {
            chatHistory.add(message);
        }
    }

    /**
     * Lấy danh sách toàn bộ lịch sử tin nhắn
     */
    public List<Message> getChatHistory() {
        return chatHistory;
    }
}


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.common;

import java.util.ArrayList;
import java.util.List;


public class Group {
    private String name;
    private String ipAddress;
    private final List<Message> messageList;

    public Group(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.messageList = new ArrayList<>();
    }

    public String getName() { return name; }
    public String getIpAddress() { return ipAddress; }
    
    // Hàm lấy toàn bộ lịch sử tin nhắn
    public synchronized List<Message> getMessageList() { return messageList; }

    // Hàm thêm tin nhắn mới vào phòng
    public synchronized void addMessage(Message msg) {
        this.messageList.add(msg);
    }

    @Override
    public String toString() {
        return name; 
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package dev.ansida.chat.client;

import dev.ansida.chat.common.Group;
import javax.swing.DefaultListModel;

public class GroupSelectionPanel extends javax.swing.JPanel {

    public interface GroupSelectionListener {

        void onGroupSelected(Group selectedGroup);
    }

    private GroupSelectionListener listener;

    // Khai báo Model quản lý danh sách Group động thay cho mảng String mặc định của NetBeans
    private DefaultListModel<Group> listModel;

    public GroupSelectionPanel() {
        initComponents();
        initCustomComponents();
    }

    private void initCustomComponents() {
        // 1. Tạo Model danh sách trống chuyên chứa các Object Group
        listModel = new DefaultListModel<>();
        groupList.setModel(listModel);

        // Chỉ cho phép chọn 1 phòng mỗi lần click
        groupList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        // 2. BẮT SỰ KIỆN CLICK CHỌN DÒNG TRÊN JLIST
        groupList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                // getValueIsAdjusting() == false nghĩa là sự kiện click chuột đã nhả ra hoàn toàn
                if (!e.getValueIsAdjusting() && listener != null) {
                    Group selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        // Kích hoạt callback truyền Object Group ra cho ChatController xử lý
                        listener.onGroupSelected(selectedGroup);
                    }
                }
            }
        });
    }

    // HÀM ĐĂNG KÝ LISTENER (Được gọi từ MainFrame để hứng sự kiện đổi phòng)
    public void setGroupSelectionListener(GroupSelectionListener listener) {
        this.listener = listener;
    }
    
    // HÀM THÊM PHÒNG CHAT MỚI TỪ BACKEND ĐẨY VÀO PANEL
    public void addGroup(Group group) {
        if (group != null) {
            listModel.addElement(group);
        }
    }

    // HÀM TIỆN ÍCH LÀM SẠCH DANH SÁCH PHÒNG NẾU CẦN
    public void clearGroups() {
        listModel.clear();
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        groupList = new javax.swing.JList<>();

        jScrollPane1.setViewportView(groupList);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(97, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(130, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<Group> groupList;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.ansida.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {
    public static final int PORT = 31323;
    public static final ExecutorService es = Executors.newCachedThreadPool();
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    public static void main(String[] args) {
        logger.log(Level.INFO, "Server đang khởi động tại cổng {0}...", PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.log(Level.INFO, "Server đã sẵn sàng lắng nghe kết nối!");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                logger.log(Level.INFO, "Co ket noi moi tu: {0}", socket.getRemoteSocketAddress());
                ClientHandler ch = new ClientHandler(socket);
                es.submit(ch);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Lỗi ServerSocket hoặc ngắt kết nối đột ngột", ex);
        } finally {
            es.shutdown();
            logger.log(Level.INFO, "Server đã dừng hoạt động.");
        }
    }
}


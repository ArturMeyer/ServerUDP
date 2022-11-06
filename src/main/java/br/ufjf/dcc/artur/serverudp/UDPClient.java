/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.ufjf.dcc.artur.serverudp;

import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Timer;

import javax.swing.JFileChooser;

/**
 *
 * @author Artur Welerson Sott Meyer
 */
public class UDPClient {

    private static String HOST = "127.0.0.1";
    private static int PORT = 4449;

    public static void sendMessage(byte[] buf, DatagramSocket socket) throws UnknownHostException, IOException {
        InetAddress ip = InetAddress.getByName("127.0.0.1");

        DatagramPacket out = new DatagramPacket(buf, buf.length, ip, PORT);
        System.out.println("Mensagem enviada ");
        socket.send(out);
    }

    public static void main(String[] args) {

        try {
            while (true) {
                System.out.println("Opening file manager...");

                File file = null;
                JFileChooser fileChooser = new JFileChooser();

                float size = 0;

                int respostFileChooser = fileChooser.showOpenDialog(null);

                if (respostFileChooser == JFileChooser.APPROVE_OPTION) {
                    file = fileChooser.getSelectedFile();
                    size = (float) (file.length() / 1024.0);
                    if (size > 64) {
                        System.out.println("Very large file!");
                        continue;
                    }
                    System.out.println("Caminho do arquivo " + file.getPath() + " - " + size + "KB");
                }

                byte[] content = new byte[(int) file.length()];
                FileInputStream fileBytes = new FileInputStream(file);
                fileBytes.read(content);
                fileBytes.close();

                UDPFile fileUDP = new UDPFile();
                fileUDP.setContent(content);
                fileUDP.setDate(new Date());
                fileUDP.setName(file.getName());
                fileUDP.setSize((int) size);

                ByteArrayOutputStream arrayByte = new ByteArrayOutputStream();
                ObjectOutputStream objectByte = new ObjectOutputStream(arrayByte);
                objectByte.writeObject(fileUDP);

                byte[] buf = arrayByte.toByteArray();

                DatagramSocket socket = new DatagramSocket();

                InetAddress ip = InetAddress.getByName("127.0.0.1");

                DatagramPacket out = new DatagramPacket(buf, buf.length, ip, PORT);
                
                sendMessage(buf, socket);

                byte[] bufReceive = new byte[100];
                DatagramPacket in = new DatagramPacket(bufReceive, bufReceive.length);

                Message send = new Message(socket, in, buf, System.currentTimeMillis(), PORT);
                Thread threadSend = new Thread(send);

                Thread.sleep(Delay.delay);
                long timeInit = System.currentTimeMillis();
                socket.receive(in);
                Delay.delay = System.currentTimeMillis() - timeInit;
                System.out.println(Delay.delay);

                threadSend.interrupt();

                String messageReceive = new String(in.getData());
                System.out.println(messageReceive);
            }

        } catch (SocketException ex) {
            Logger.getLogger(UDPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(UDPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UDPClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(UDPClient.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}

class Delay {

    public static long delay = 5000;

}

class Message implements Runnable {

    private DatagramSocket socket;
    private DatagramPacket in;
    private int PORT;
    private byte[] buf;
    private long initialTime;
    private int send;

    public void sendMessage(byte[] buf, DatagramSocket socket) throws UnknownHostException, IOException {
        InetAddress ip = InetAddress.getByName("127.0.0.1");

        DatagramPacket out = new DatagramPacket(buf, buf.length, ip, PORT);

        socket.send(out);

        send++;

    }

    public void run() {
        if (Thread.interrupted()) {
            return;
        }
        while (true) {
            if (Thread.interrupted()) {
                break;
            }
            if (System.currentTimeMillis() - initialTime > Delay.delay) {
                try {
                    if (send >= 5) {
                        System.out.println("Numero de tentativas de envios excedidas");
                        break;
                    }
                    System.out.println(send + " - Message forwarded");
                    sendMessage(buf, socket);
                    initialTime = System.currentTimeMillis();
                } catch (IOException ex) {
                    Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        socket.close();
    }

    public Message(DatagramSocket socket, DatagramPacket in, byte[] buf, long initialTime, int PORT) {
        this.socket = socket;
        this.in = in;
        this.buf = buf;
        this.initialTime = initialTime;
        this.PORT = PORT;
    }

}

class UDPFile implements Serializable {

    private String name;
    private byte[] content;
    private transient long size;
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

}

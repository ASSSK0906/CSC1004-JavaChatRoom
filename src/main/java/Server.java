import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class Server {
    private ServerSocket ss;
    private String chatRoomId;
    private BufferedWriter bw;

    public Server(ServerSocket ss) throws IOException {
        this.ss = ss;
        this.chatRoomId = UUID.randomUUID().toString();
        this.bw = new BufferedWriter(new FileWriter("chat_history.txt", true));
        System.out.println();
        System.out.println("Chatroom " + this.chatRoomId + " is created!");
    }

    public void logHistory(String msg) throws IOException {
        this.bw.write(msg);
        this.bw.newLine();
        this.bw.flush();
    }

    public void startServer() throws IOException {
        while(!this.ss.isClosed()) {
            Socket s = this.ss.accept();
            System.out.println("\nNew client request received : " + String.valueOf(s));
            ClientHandler clientHandler = new ClientHandler(s, this);
            Thread thread = new Thread(clientHandler);
            thread.start();
        }

    }

    public void close() throws IOException {
        this.ss.close();
    }

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(999);
        Server s = new Server(ss);
        s.startServer();
    }
}
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {
    public static Vector<ClientHandler> clientHandlers = new Vector();
    private Socket s;
    private Server server;
    private String id;
    private String username = "";
    public static AtomicInteger idCount = new AtomicInteger(0);
    private BufferedReader reader;
    private BufferedWriter writer;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.s = socket;
        this.server = server;
        clientHandlers.add(this);
        System.out.println("Added client to active client list\n");
        this.reader = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(this.s.getOutputStream()));
    }

    private static String getClientInfo(ClientHandler ch) {
        String result = "";
        if (!ch.s.isClosed()) {
            result = result + "-" + ch.id + "|" + ch.username;
        }

        return result;
    }

    public void run() {
        this.id = String.valueOf(12345 + idCount.getAndIncrement());

        try {
            this.writer.write(this.id);
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException e) {
            System.out.println("Error clientHandler sending id");
            e.printStackTrace();
            return;
        }

        try {
            this.username = this.reader.readLine();
        } catch (IOException e) {
            System.out.println("Error clientHandler getting username");
            e.printStackTrace();
            return;
        }

        for(ClientHandler ch : clientHandlers) {
            if (!ch.s.isClosed() && !ch.username.isEmpty()) {
                try {
                    this.writer.write(getClientInfo(ch));
                    this.writer.newLine();
                    this.writer.flush();
                } catch (IOException e) {
                    System.out.println("Error ClientHandler sending client's info");
                    e.printStackTrace();
                    return;
                }
            }
        }

        try {
            this.writer.write("END");
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String messageFromClient;
        for(; !this.s.isClosed(); this.broadcastMessage("[MESSAGE] " + messageFromClient)) {
            messageFromClient = "";

            try {
                messageFromClient = this.reader.readLine();
                if (messageFromClient.equals("Quit Request")) {
                    String leaveNotice = "[SERVER: " + this.id + "|" + this.username + " has left the chat room !]";
                    System.out.println(leaveNotice);
                    this.s.close();
                    this.broadcastMessage(leaveNotice);
                    break;
                }

                System.out.println("[MESSAGE] " + messageFromClient);
                this.server.logHistory("[HISTORY] " + messageFromClient);
            } catch (IOException e) {
                System.out.println("Error clientHandler reading");
                e.printStackTrace();
                return;
            }
        }

    }

    public void broadcastMessage(String messageToSend) {
        for(ClientHandler ch : clientHandlers) {
            if (!ch.s.isClosed()) {
                try {
                    ch.writer.write(messageToSend);
                    ch.writer.newLine();
                    ch.writer.flush();
                } catch (IOException e) {
                    System.out.println("Error clientHandler writing");
                    e.printStackTrace();
                    return;
                }
            }
        }

    }
}


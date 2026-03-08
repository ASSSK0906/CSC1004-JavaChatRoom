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



    public void run() {
        /*Step1: generate id and send it to Client side*/
        this.id = String.valueOf(12345 + idCount.getAndIncrement());
        //send out id
        try {
            this.writer.write(this.id);
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException e) {
            System.out.println("Error clientHandler sending id");
            e.printStackTrace();
            return;
        }
        /*Step2: receive name from Client side*/
        try {
            this.username = this.reader.readLine();
        } catch (IOException e) {
            System.out.println("Error clientHandler getting username");
            e.printStackTrace();
            return;
        }

        /*Step3: generate client info: id|name and send it to Client side*/
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
        //Use "END" as a sign to stop reading
        try {
            this.writer.write("END");
            this.writer.newLine();
            this.writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /*Step4: receive message from Client side*/
        String messageFromClient;
        for(; !s.isClosed(); broadcastMessage("[MESSAGE] " + messageFromClient)) {
            try {
                //read message
                messageFromClient = reader.readLine();

                /*SPECIAL CASE: quit request*/
                if (messageFromClient.equals("Quit Request")) {
                    //print out and send leaving message
                    String leaveNotice = "[SERVER: " + this.id + "|" + this.username + " has left the chat room !]";
                    System.out.println(leaveNotice);
                    s.close();
                    broadcastMessage(leaveNotice);
                    break;
                }
                //print out message
                System.out.println("[MESSAGE] " + messageFromClient);


                /*Step4: record chat history*/
                server.logHistory("[HISTORY] " + messageFromClient);
            } catch (IOException e) {
                System.out.println("Error clientHandler reading");
                e.printStackTrace();
                return;
            }
        }

    }

    //helper method to format client info: id|name
    private static String getClientInfo(ClientHandler ch) {
        String result = "";
        if (!ch.s.isClosed()) {
            result = result + "-" + ch.id + "|" + ch.username;
        }

        return result;
    }

    //helper method to send message to all clients
    public void broadcastMessage(String messageToSend) {
        //Iterate through active client
        for(ClientHandler ch : clientHandlers) {
            if (!ch.s.isClosed()) {
                try {
                    //send message
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


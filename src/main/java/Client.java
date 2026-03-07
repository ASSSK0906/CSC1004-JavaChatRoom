import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Scanner;

public class Client {
    private Socket s;
    private String id;
    private String username;
    private static Scanner scn;
    private BufferedReader reader;
    private BufferedWriter writer;
    private BufferedReader fileReader;

    public Client(Socket socket, String username) throws IOException {
        this.s = socket;
        this.username = username;
        this.reader = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(this.s.getOutputStream()));
        this.fileReader = new BufferedReader(new FileReader("chat_history.txt"));
    }

    private void assignedId(String id) {
        this.id = id;
    }

    public void sendMessage() {
        (new Thread(new Runnable() {
            {
                Objects.requireNonNull(Client.this);
            }

            public void run() {
                while(true) {
                    if (!Client.this.s.isClosed()) {
                        String msg = Client.scn.nextLine();
                        if (!msg.equals("Quit")) {
                            try {
                                LocalDateTime now = LocalDateTime.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                                String formattedTime = now.format(formatter);
                                Client.this.writer.write(Client.this.id + "|" + Client.this.username + ": " + msg + " (" + formattedTime + ")");
                                Client.this.writer.newLine();
                                Client.this.writer.flush();
                                continue;
                            } catch (IOException e) {
                                System.out.println("Error client writing");
                                e.printStackTrace();
                                return;
                            }
                        }

                        try {
                            Client.this.writer.write("Quit Request");
                            Client.this.writer.newLine();
                            Client.this.writer.flush();
                            Client.this.s.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    return;
                }
            }
        })).start();
    }

    public void listenForMessage() {
        (new Thread(new Runnable() {
            {
                Objects.requireNonNull(Client.this);
            }

            public void run() {
                String msgFromGroupChat = "";

                while(!Client.this.s.isClosed()) {
                    try {
                        if (msgFromGroupChat == null) {
                            break;
                        }

                        msgFromGroupChat = Client.this.reader.readLine();
                        System.out.println(msgFromGroupChat);
                    } catch (IOException e) {
                        if (Client.this.s.isClosed()) {
                            System.out.println("Connection closed by server.");
                        } else {
                            System.out.println("Error client reading: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    }
                }

            }
        })).start();
    }

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 999);
        System.out.println("Please enter your username for group chat: ");
        String username = Client.scn.nextLine();
        Client c = new Client(s, username);
        c.writer.write(username);
        c.writer.newLine();
        c.writer.flush();
        c.assignedId(c.reader.readLine());
        System.out.println("Welcome " + c.id + "|" + c.username + "!\nType 'Quit' to leave the chat room\n");
        System.out.println("Current active client: ");

        String clientInfo;
        while(!(clientInfo = c.reader.readLine()).equals("END")) {
            System.out.println(clientInfo + "\n");
        }

        System.out.println("Chat history of this room:");
        Scanner scn = new Scanner(new File("chat_history.txt"));
        String history = "";

        while(scn.hasNext()) {
            history = scn.nextLine();
            System.out.println(history);
        }

        if (history.isEmpty()) {
            System.out.println("This room has no history now\n");
        }

        c.sendMessage();
        c.listenForMessage();
    }

    static {
        scn = new Scanner(System.in);
    }
}

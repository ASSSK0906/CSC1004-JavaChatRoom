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
    private static Scanner scn = new Scanner(System.in);
    private BufferedReader reader;
    private BufferedWriter writer;

    public Client(Socket socket, String username) throws IOException {
        this.s = socket;
        this.username = username;
        this.reader = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(this.s.getOutputStream()));
    }

    public void sendMessage() {
        (new Thread(() -> {
            while(true) {
                if (!s.isClosed()) {
                    /*Step1: scan message from console typed in*/
                    String msg = Client.scn.nextLine();


                    if (!msg.equals("Quit")) {
                        /*Step2: add time stamp*/
                        try {
                            LocalDateTime now = LocalDateTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                            String formattedTime = now.format(formatter);
                            /*Step3: send the message*/
                            writer.write(id + "|" + username + ": " + msg + " (" + formattedTime + ")");
                            writer.newLine();
                            writer.flush();
                            continue;
                        } catch (IOException e) {
                            System.out.println("Error client writing");
                            e.printStackTrace();
                            return;
                        }
                    }

                    /*SPECIAL CASE: quit message*/
                    try {
                        writer.write("Quit Request");
                        writer.newLine();
                        writer.flush();
                        s.close();
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                return;
            }
        })).start();
    }

    public void listenForMessage() {
        (new Thread(() -> {
            String msgFromGroupChat = "";

            while(!s.isClosed()) {
                try {
                    if (msgFromGroupChat == null) {
                        break;
                    }
                    /*Step1: receive message from Server side*/
                    msgFromGroupChat = reader.readLine();
                    System.out.println(msgFromGroupChat);
                } catch (IOException e) {
                    if (s.isClosed()) {
                        //This exception is for client quitting
                        //Since when socket is being closed, listenForMessage thread is working
                        //an exception will be created at the same time.
                        System.out.println("Connection closed by server.");
                    } else {
                        System.out.println("Error client reading: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                }
            }

        })).start();
    }

    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 999);
        System.out.println("Please enter your username for group chat: ");

        /*Step1: Send out name to Server side, and Receive id from Server side*/
        String username = Client.scn.nextLine();
        Client c = new Client(s, username);

        //send name
        c.writer.write(username);
        c.writer.newLine();
        c.writer.flush();

        //get id
        c.id = c.reader.readLine();
        System.out.println("Welcome " + c.id + "|" + c.username + "!\nType 'Quit' to leave the chat room\n");
        System.out.println("Current active client: ");

        /*Step2: receive client info(id + name) and print out active clients*/
        String clientInfo;
        while(!(clientInfo = c.reader.readLine()).equals("END")) {
            System.out.println(clientInfo + "\n");
        }

        /*Step3: Read and print out chat history vai file*/
        System.out.println("Chat history of this room:");
        Scanner scn = new Scanner(new File("chat_history.txt"));
        String history = "";
        //use Scanner to read line by line
        while(scn.hasNext()) {
            history = scn.nextLine();
            System.out.println(history);
        }
        //special case: no history yet
        if (history.isEmpty()) {
            System.out.println("This room has no history now\n");
        }

        /*Step4: send and listen threads start*/
        c.sendMessage();
        c.listenForMessage();
    }
}

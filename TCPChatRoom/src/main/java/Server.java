import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(8080);
            pool = Executors.newCachedThreadPool();
            System.out.println("Server is running...");
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch == null) continue;
            ch.sendMessage(message);
        }
    }

    public void shutdown() {
        if (server == null) return;

        try {
            System.out.println("Server is shutting down...");
            done = true;
            pool.shutdown();
            if (!server.isClosed()) server.close();
            for (ConnectionHandler ch : connections) {
                if (ch == null) continue;
                ch.shutdown();
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    class ConnectionHandler implements Runnable {

        private final Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            System.out.println("Handling connection...");
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter a nickname: ");
                nickname = in.readLine();
                if (nickname == null) {
                    // TODO: add validation to nickname
                    return;
                }
                System.out.println(nickname + " has connected.");
                broadcast(nickname + " joined the chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            String newNickname = messageSplit[1];
                            broadcast(nickname + " changed their nickname to " + newNickname);
                            System.out.println("Successfully changed nickname from " + nickname + " to " + newNickname);
                            nickname = newNickname;
                        } else {
                            out.println("No nickname provided.");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname + " left the chat!");
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            if (client == null) return;

            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
                System.out.println(nickname + " has disconnected.");
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}

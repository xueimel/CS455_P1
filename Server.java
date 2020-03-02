import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class Server {
    private int port;
    private Map names; // Nickname: IP...or reverse
    private Map rooms; // Room: [participants]
    private static Socket s;
    private static ServerSocket ss;

    public Server(int port) throws IOException {
        this.port = port;
        ss = new ServerSocket(port);
        System.out.println("starting server\n");

        while (true)
            new ServerConnection(ss.accept()).start();
    }

    /**
     * The main server method that accepts connections and starts off a new thread
     * to handle each accepted connection.
     */
    public synchronized static void runServer() {
        Socket client;
        try {
            while (true) {
                client = ss.accept();
                System.out.println("Received connection starting game");
                new ServerConnection(client).start();

            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }


    public static void main(String args[]) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java Server <serverHost> <port#>"); // local host
            System.exit(1);
        }

        try {
            Server server = new Server(Integer.parseInt(args[1]));
            Server.runServer();
        } catch (NumberFormatException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

    }
}

class ServerConnection extends Thread
{
    private Socket client;

    ServerConnection(Socket client) throws SocketException{
        this.client = client;
        setPriority(NORM_PRIORITY - 1);
    }

    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            System.out.println("Connection with client " + client.getInetAddress().getHostAddress());
            while (true) {
                //an if statment could be here from the clients room
                // I THINK THIS MAY BE A PROBLEM IN SPECIFYING THAT THE OBJECT SHOULD ONLY GO TO X RECIPIENTS.
                // PERHAPS JUST GIVING THEM A NULL IF THEY ARE NOT IN THE ROOM A MESSAGE WAS MEANT FOR
                out.writeObject(processRequest(in.readObject()));
                out.flush();
                Thread.sleep(3000);
                Message x = new Message("occasional Hello");
                out.writeObject(x);
                out.flush();
            }
        } catch (EOFException e) { // Normal EOF
            try {
                client.close();
            } catch (IOException e1) {
                System.err.println(e1);
            }
        } catch (IOException e) {
            System.err.println("I/O error " + e); // I/O error
        } catch (ClassNotFoundException e) {
            System.err.println(e); // Unknown type of request object
        } catch (InterruptedException e){
            System.err.println(e); // Unknown type of request object

        }
    }

    private Object processRequest(Object request) {
        if (request instanceof Message) { // got message from client
            Message mess = ((Message) request);
            // Handle logic for message.
            // Check to see that the client is in a room
            // Forward the message to other clients in the "room"
            Message new_m = new Message("Hello from server");
            return new_m;
        }
        else if (request instanceof IRC) {
            IRC command = ((IRC) request);  // got command from client
            // logic for the returning the command in a message
            Message mess = new Message("HOT MESS");

            return mess;
        }
        else
            return null;
    }
}


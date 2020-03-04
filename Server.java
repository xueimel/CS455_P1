import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Server {
    // TODO:
    /* TODO: Finish the 'broadcast' or outgoing message service which deliver to everyone in their room. Pretty sure
    that we need to map the outgoing streams to the clients in the rooms */
    private int port;
    public static Map names; // Nickname: IP...or reverse
    public static Map rooms; // Room: [participants]
    private static ServerSocket ss;

    public Server(int port) throws IOException {
        names = Collections.synchronizedMap(new HashMap<String, String>());
        rooms = Collections.synchronizedMap(new HashMap<String, LinkedList<ObjectOutputStream>>());
        this.port = port;
        ss = new ServerSocket(port);
        System.out.println("starting server\n");
    }
    /**
     * The main server method that accepts connections and starts off a new thread
     * to handle each accepted connection.
     */
    public synchronized void runServer() {
        Socket client;
        try {
            while (true) {
                client = ss.accept();
                names.put(client.getInetAddress(), null);
//                if (rooms.get('a') == null){
//                    rooms.put('a', new LinkedList<Socket>());
//                }
//                LinkedList list = (LinkedList) rooms.get('a');
//                list.add(client);
//                rooms.put('a', list);
//                rooms.put('a', client.getInetAddress());
                new ServerConnection(client, this).start();
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
            server.runServer();
//            Server.runServer();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}

class ServerConnection extends Thread
{
    private Socket client;
    private Server waiter;

    ServerConnection(Socket client, Server guy){
        this.waiter = guy;
        this.client = client;
        setPriority(NORM_PRIORITY - 1);
    }

    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            if (waiter.rooms.get('a') == null){
                waiter.rooms.put('a', new LinkedList<ObjectOutputStream>());
            }
            LinkedList list = (LinkedList) waiter.rooms.get('a');
            list.add(out);
            waiter.rooms.put('a', list);
            System.out.println("Connection with client " + client.getInetAddress().getHostAddress());
            out.writeObject(new Message("CONNECTED TO THE SERVER")); // if you remove this, the output stream fails
            while (true) {
                //an if statment could be here from the clients room
                // I THINK THIS MAY BE A PROBLEM IN SPECIFYING THAT THE OBJECT SHOULD ONLY GO TO X RECIPIENTS.
                // PERHAPS JUST GIVING THEM A NULL IF THEY ARE NOT IN THE ROOM A MESSAGE WAS MEANT FOR
//                out.writeObject(processRequest(in.readObject(), client, out));
//                out.flush();
                makeNoise(in.readObject(), client);
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
        }
    }

    private void makeNoise(Object obj, Socket client)throws ClassNotFoundException{ //If i hand it in, it doesn't write. If I try to make a new 'out' it throws exception
        //If i hand 'out' in, it doesn't write. If I try to make a new 'out' it throws exception
        System.out.println("GOT AN OBJECT FROM: " + client.getInetAddress());
        try {
            if (obj instanceof Message) { // got message from client
                Message mess = ((Message) obj);
                LinkedList list = (LinkedList) waiter.rooms.get('a');
                for (int i = 0; i < list.size(); i++) {
                    ObjectOutputStream out = (ObjectOutputStream) list.get(i);
                    out.writeObject(new Message(mess.getString()));
                    out.flush();
                }

//            System.out.println(message.getString());
            }
            else{
                System.out.println("NOT MESSAGE");
            }
        }catch (IOException e){
            System.err.println("MAKE NOISE ERROR" + e );
        }
    }

    private Object processRequest(Object request, Socket client, ObjectOutputStream out) {
        if (request instanceof Message) { // got message from client
            Message mess = ((Message) request);
            // Handle logic for message.
            // Check to see that the client is in a room
            // Forward the message to other clients in the "room"
            Message new_m = new Message("Hello from server");
            System.out.println("HELLO I GOT A MESSAGE");

//            makeNoise(mess, client, out);
            return new_m;
        }
        else if (request instanceof IRC) {
            IRC command = ((IRC) request);  // got command from client
            // logic for the returning the command in a message
            if (command.command.equals("join")){
                //
            }
            if (command.command.equals("list")){

            }
            if (command.command.equals("connect")){
                return new Message("hot mess");
            }
            Message mess = new Message("HOT MESS");

            return mess;
        }
        else
            return null;
    }
}


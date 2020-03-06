import com.sun.org.apache.xpath.internal.SourceTree;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private int port;
    public Map names; // Nickname: IP...or reverse
    public Map rooms; // Room: [participants]
    public Map clients;
    public Map clientRoom;
    public boolean inServer = false;
    private static ServerSocket ss;

    public Server(int port) throws IOException {
        names = new HashMap<String, Socket>();
        rooms = Collections.synchronizedMap(new HashMap<String, LinkedList<Socket>>());
        clients = Collections.synchronizedMap(new HashMap<Socket, ObjectOutputStream>());
        clientRoom = Collections.synchronizedMap(new HashMap<Socket, String>()); // secondary thought about how to approach client and rooms
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
                names.put(client, null);
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
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}


class ServerConnection extends Thread
{
    private Socket client;
    private Server waiter;
    Boolean inServer = false;

    ServerConnection(Socket client, Server server){
        this.waiter = server;
        this.client = client;
        setPriority(NORM_PRIORITY - 1);
    }

    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());

            waiter.clients.put(client, out);
            //TODO take input on client side to make them actually type in the connection.
            System.out.println("Connection with client " + client.getInetAddress().getHostAddress());
            out.writeObject(new Message("Contacting Server")); //if you remove this line, the output stream will fail ahead.

            while (true) {
                handleClientObject(in.readObject(), client);
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

    private String getRoom(Socket client){
        int index=0;
        for (Object mapElement: waiter.rooms.keySet()){
            LinkedList thing = (LinkedList) waiter.rooms.get(mapElement);
            for (Object potentialClient: thing) {
                if (potentialClient==client){
                    System.out.println("CLIENT IN A ROOM");
                    return (String) waiter.rooms.get(index);
                }
                index++;
            }
        }
        System.err.println("CLIENT WAS NOT ABLE TO BE LOCATED IN A ROOM");
        return "Client not in room";
    }

    private boolean inRoom(Socket client){
        for (Object mapElement: waiter.rooms.keySet()){
            LinkedList thing = (LinkedList) waiter.rooms.get(mapElement);
            for (Object s: thing) {
                if (s==client){
                    System.out.println("CLIENT IN A ROOM");
                    return true;
                }
            }
        }
        return false;
    }

    private String getClientNick(Socket client){
        for (Object key: waiter.names.keySet()) {
            Socket found_client = (Socket) waiter.names.get(key);
            if (found_client == client) {
                System.out.println("IN getClientNick(); GOT NICKNAME " + key);
                return (String) key;
            }
        }
        System.out.println("IN getClientNick(); DIDN'T FIND NICKNAME" );
        return "Anonymous";
    }

    private void handleClientObject(Object obj, Socket client){
        System.out.println("GOT AN OBJECT FROM: " + client);
        try {
            if (obj instanceof Message) { // got message from client
                System.out.println("GOT A MESSAGE FROM: " + client);
                Message mess = ((Message) obj);
                boolean roomCheck = inRoom(client); // could be simplified with clientRoom

                if (roomCheck) {
                    LinkedList list = (LinkedList) waiter.rooms.get(waiter.clientRoom.get(client));

                    // send message to all clients in the client's room
                    String name = getClientNick(client);

                    for (int i = 0; i < list.size(); i++) {
                        if (client != list.get(i)) {
                            ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(list.get(i));
                            out.writeObject(new Message(name + ": "+mess.getString()));
                            out.flush();
                        }
                    }
                }
                else{
                    ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(client);
                    if(inServer){
                        out.writeObject(new Message(client.getInetAddress() + ":YOU MUST JOIN A ROOM TO ENTER A MESSAGE [/join <room name>]" ));
                        out.flush();
                    }
                    else{
                        out.writeObject(new Message(client.getInetAddress() + "Use /connect <servername> command to start" ));
                        out.flush();
                    }
                    out.writeObject(new Message("YOU MUST JOIN A ROOM TO ENTER A MESSAGE"));
                    out.flush();
                }
            }
            // TODO everything here can be abstracted into functions and return the message.
            // The reason for this discomfort is that objectoutstream seems to only work from this function.
            // i dont understand why
            else if (obj instanceof IRC) {
                System.out.println("GOT AN IRC COMMAND FROM " + client);
                ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(client);
                IRC command = ((IRC) obj);  // got command from client
                System.out.println("COMMAND " + command.command);


                if (command.command.contains("/join")){
                    // or with addition of clientRoom you could just see if the client exists in the list
                    boolean in = inRoom(client);
                    if (!in) { // make sure the client is only in one room.
                        Message m;
                        String room = command.command.substring(5);
                        if (room.trim().length()==0){ //no room name given
                            System.out.println("CLIENT ATTEMPTED TO JOIN ROOM WITHOUT SPECIFYING A NAME");
                            m = new Message("YOU MUST ENTER THE NAME OF THE ROOM YOU WISH TO JOIN (Ex. /join music room)");
                            out.writeObject(m);
                            out.flush();
                        }
                        else if (waiter.rooms.get(room) == null) { // make room if it doesnt exist
                            waiter.rooms.put(room, new LinkedList<Socket>());
                            m = new Message("Room " + room + " was created");
                            waiter.clientRoom.put(client, room);
                        } else {
                            m = new Message("You have joined " + room);
                            waiter.clientRoom.put(client, room);
                        }

                        // reformat the rooms list
                        LinkedList list = (LinkedList) waiter.rooms.get(room);
                        list.add(client);
                        waiter.rooms.put(room, list);
                        out.writeObject(m);
                        out.flush();
                    }
                    else{
                        System.out.println("CLIENT ATTEMPT TO ENTER MULTIPLE ROOMS");
                        out.writeObject(new Message("YOU MUST EXIT THE CURRENT ROOM BEFORE JOINING ANOTHER"));
                        out.flush();
                    }
                }
                else if (command.command.contains("/leave")) {
                    Message m;
                    String room = command.command.substring(6);
                    if (room.trim().length() == 0){
                        System.out.println("CLIENT ATTEMPTED TO LEAVE ROOM WITHOUT SPECIFYING A NAME");
                        m = new Message("YOU MUST ENTER THE NAME OF THE ROOM YOU WISH TO LEAVE (Ex. /leave music room)");
                        out.writeObject(m);
                        out.flush();
                        return;
                    }
                    boolean inTheRoom = false;
                    for (Object mapElement: waiter.rooms.keySet()){ // make sure client in the given room
                        if (mapElement.toString().equals(room)) {
                            inTheRoom = true;
                        }
                    }
                    if (inTheRoom){
                        System.out.println("Client " + client + " Leaving room " + room);
                        LinkedList list = (LinkedList) waiter.rooms.get(room);
                        list.remove(client);
                        waiter.rooms.put(room, list);
                        waiter.clientRoom.remove(client);
                        out.writeObject(new Message("YOU HAVE SUCCESSFULLY LEFT ROOM"+ room));
                        out.flush();
                    }
                    else{
                        out.writeObject(new Message("YOU ARE NOT CURRENTLY IN THE ROOM YOU SPECIFIED"));
                        out.flush();
                    }
                }
                else if (command.command.contains("/list")){
                    System.out.println("LISTING ROOMS FOR " + client);
                    Message m = new Message(waiter.rooms.keySet().toString().replace("[", "").replace("]", ""));
                    out.writeObject(m);
                    out.flush();
                }
                else if (command.command.contains("/connect")) {
                    if(!inServer){
                        System.out.println("CLIENT ATTEMPTING A CONNECT " + client);
                        out.writeObject(new Message("YOU HAVE CONNECTED TO SERVER"));
                        inServer = true;

                    }
                    else{ //IM A FEATRUE NOT A BUG
                        System.out.println("CLIENT ATTEMPTING A RECONNECT " + client);
                        out.writeObject(new Message("CONNECTED!"));
                    }
                    out.flush();
                }
                else if (command.command.contains("/nick")){
                    System.out.println("CLIENT ATTEMPTING TO ADD/ALTER NICKNAME " + client);
                    Message m;
                    String name = command.command.substring(5);
                    if (!waiter.names.containsKey(name)) { // if name doesn't already exist
                        System.out.println("CLIENT " + client + " ADDED THE NICKNAME " + name);
                        waiter.names.put(name, client);
                        out.writeObject(new Message("Great to see you," + name + ". Your nickname has been added." ));
                        out.flush();
                    }else{
                        System.out.println("CLIENT " + client + " NICKNAME ALREADY TAKEN " + name);
                        out.writeObject(new Message("Sorry"+ name + " is already taken. Please choose another nickname"));
                        out.flush();
                    }
                }
                else if (command.command.contains("/quit")) {
                    waiter.names.remove(getClientNick(client));
                    System.out.println("CLIENT REMOVED FROM NAMES");
                    //TODO remove client from room
//                    String room = getRoom(client);
                    if(inRoom(client)) { //If client in room
                        String room = (String) waiter.clientRoom.get(client);
                        waiter.clientRoom.remove(client);
                        System.out.println("ROOM BEFORE ATTEMPTING REMOVE " + waiter.rooms.get(room));
                        LinkedList roomList = (LinkedList) waiter.rooms.get(room);
                        roomList.remove(client);
                        System.out.println("REMOVED CLIENT FROM ROOM");
                        System.out.println("ROOM AFTER ATTEMPTING REMOVE " + waiter.rooms.get(room));
                        waiter.rooms.put(room, roomList);
                    }
                    out.writeObject(new Message("YOU HAVE SUCCESSFULLY QUIT"));
                    out.flush();
                    //disconnect socket
//                    client.close(); TODO once client side is figured out, should be able to do this
                    System.out.println("CLIENT QUITING " + client);
                }
                else if (command.command.contains("/stats")) {
                    //TODO print off len list in rooms
                    //num of clients connected
                    //potentially timer stats
                    //maybe counter for num of times messages received
                }
                else{
                    System.err.println("CLIENT " + client + " GAVE A BAD COMMAND " + "\""+command.command+"\"");
                }
            }
            else{
                System.err.println("OBJECT FROM "+ client+ " WAS NOT MESSAGE or IRC COMMAND. OBJECT WAS: " + obj.getClass());
            }
        }catch (IOException e){
            System.err.println("HOLY HELL THAT SHOULDN'T HAVE HAPPENED" + e );
        }
    }
}


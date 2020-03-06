import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private int port;
    public Map names; // Nickname: IP...or reverse
    public Map rooms; // Room: [participants]
    public Map clients;
    public boolean inServer = false;
    private static ServerSocket ss;

    public Server(int port) throws IOException {
        names = Collections.synchronizedMap(new HashMap<Socket, String>());
        rooms = Collections.synchronizedMap(new HashMap<String, LinkedList<Socket>>());
        clients = Collections.synchronizedMap(new HashMap<Socket, ObjectOutputStream>());
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
            System.out.println("Connection with client " + client.getInetAddress().getHostAddress());

            while (true) {
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


    private void makeNoise(Object obj, Socket client) throws ClassNotFoundException{
        System.out.println("GOT AN OBJECT FROM: " + client);
        try {
            //this is causing an error to be thrown
            //line 120
            if (obj instanceof Message) { // got message from client
                System.out.println("GOT A MESSAGE FROM " + client);
                Message mess = ((Message) obj);
                boolean roomCheck = inRoom(client);
                    if (roomCheck) { //TODO: Faults after joined new room and writing a message

                        for (Object mapElement: waiter.rooms.keySet()){
                            LinkedList thing = (LinkedList) waiter.rooms.get(mapElement);
                            for (Object s: thing) {
                                if (s!=client){
                                    ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(mapElement);
                                    out.writeObject(new Message(client.getInetAddress()+ ":" + mess.getString()));
                                    out.flush();
                                }
                            }
                        }
                        // LinkedList list = (LinkedList) waiter.rooms.values(); //TODO
                        // for (int i = 0; i < list.size(); i++) {
                        //     if (client != list.get(i)) {
                        //         ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(list.get(i));
                        //         out.writeObject(new Message(client.getInetAddress()+ ":" + mess.getString()));
                        //         out.flush();
                        //     }
                        // }
                    }
                    else{
                        //if the user isnt connected to a server yet go to /command execution
                        //this could cause issues if someone tried to join a room before they connect to the server
                        ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(client);
                        
                        if(inServer){
                        out.writeObject(new Message(client.getInetAddress() + ":YOU MUST JOIN A ROOM TO ENTER A MESSAGE [/join <room name>]" ));  
                        out.flush();
                        }
                        else{
                            out.writeObject(new Message(client.getInetAddress() + "Use /connect <servername> command to start" ));  
                            out.flush();
                        }
                    }
                    //}
            }
            else if (obj instanceof IRC) {
                System.out.println("GOT AN IRC COMMAND FROM " + client);
                ObjectOutputStream out = (ObjectOutputStream) waiter.clients.get(client);
                IRC command = ((IRC) obj);  // got command from client
                System.out.println("COMMAND " + command.command);

                if (command.command.contains("/join")){
                    boolean in = inRoom(client);
                    if (!in) { // make sure the client is only in one room.
                        Message m;
                        //TODO: Dont throw execption with empty room parameter: COMPLETED
                        String room = command.command.substring(5);
                        if (room.length()<1)
                        {
                            m = new Message("INVALID ROOM NAME: " + room);
                            out.writeObject(m);
                            out.flush();
                        }
                        else{
                            if (waiter.rooms.get(room) == null) { // make room if it doesnt exist
                                waiter.rooms.put(room, new LinkedList<Socket>());
                                m = new Message("Room " + room + " was created and joined");
                            } else {
                                m = new Message("You have joined " + room);
                            }
                        
                        // reformat the rooms list
                        LinkedList list = (LinkedList) waiter.rooms.get(room);
                        list.add(client);
                        waiter.rooms.put(room, list);
                        out.writeObject(m);
                        out.flush();
                        }
                    }
                    else{
                        System.err.println("CLIENT ATTEMPT TO ENTER MULTIPLE ROOMS");
                        out.writeObject(new Message("YOU MUST EXIT THE CURRENT ROOM BEFORE JOINING ANOTHER"));
                        out.flush();
                    }
                }
                else if (command.command.contains("/leave")) {
                    Message m;
                    //its better to just find the room from the client infromation but alright
                    //String room = command.command.split(" ")[1]; //TODO will throw an array exception if no room given
                    String room = command.command.substring(6);
                    if (room.length()<1)
                    {
                        m = new Message("INVALID ROOM NAME: " + room);
                        out.writeObject(m);
                        out.flush();
                    }
                    else{
                        boolean inTheRoom = false;
                        for (Object mapElement: waiter.rooms.keySet()){ // make sure client in the given room
                                System.err.println("MAP ELEM " + mapElement.toString());
                            if (mapElement.toString().equals(room)) {
                                inTheRoom = true;
                            }
                        }
                        System.out.println(inTheRoom);
                        if (inTheRoom){
                            //TODO Test
                            LinkedList list = (LinkedList) waiter.rooms.get(room);
                            System.out.println(list);
                            System.out.println(client);
                            list.remove(client); 
                            waiter.rooms.put(room, list);

                            out.writeObject(new Message("YOU HAVE SUCCESSFULLY LEFT THE ROOM"));
                            out.flush();
                        }
                        else{
                            out.writeObject(new Message("YOU ARE NOT CURRENTLY IN THE ROOM YOU SPECIFIED"));
                            out.flush();
                        }
                    }
                }
                else if (command.command.contains("/list")){
                    System.out.println("LISTING ROOMS FOR " + client);
                    //get rooms 

                    //for each room 
                    //LinkedList l = waiter.rooms.keySet();
                        //count how many users
                            // similar to how it is done in messenger 
                    //you might have to make a for each loop that is able to go t
                    // through the  room names and users to add them to an array to print out
                    Message m = new Message(waiter.rooms.keySet().toString());
                    out.writeObject(m);
                    out.flush();
                }
                else if (command.command.contains("/connect")) {
                    if(inServer == false){
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
                    //TODO
                    System.out.println("CLIENT ATTEMPTING TO ADD/ALTER NICKNAME " + client);

                }
                else if (command.command.contains("/quit")) {
                    //TODO
                    System.out.println("CLIENT QUITING " + client);
                }
                else if (command.command.contains("/stats")) {
                    //TODO
                }
                else{
                    System.err.println("CLIENT " + client + " GAVE A BAD COMMAND " + "\""+command.command+"\"");
                    out.writeObject(new Message("INVALID COMMAND GIVEN"));
                    out.flush();
                }
            }
            else{
                System.err.println("NOT MESSAGE or IRC");
            }
        }catch (IOException e){
            System.err.println("HOLY HELL THAT SHOULDN'T HAVE HAPPENED" + e );
        }
    }
}


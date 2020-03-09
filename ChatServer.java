import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.*;

public class ChatServer {
    public Map<Socket, String> names; // Nickname: IP...or reverse
    public Map <String, LinkedList<Socket>> rooms; // Room: [participants]
    public Map <Socket, ObjectOutputStream> clients;
    public Map <Socket, String> clientRoom;
    public boolean inServer = false;
    private int debugLevel;
    private static ServerSocket ss;
    public static TimerTask tt;
    public static Timer timer;

    public ChatServer(int port, int debugLevel) throws IOException {
        this.debugLevel = debugLevel;
        names =  Collections.synchronizedMap(new HashMap<>());
        rooms = Collections.synchronizedMap(new HashMap<String, LinkedList<Socket>>());
        clients = Collections.synchronizedMap(new HashMap<Socket, ObjectOutputStream>());
        clientRoom = Collections.synchronizedMap(new HashMap<Socket, String>()); // secondary thought about how to approach client and rooms


        tt = new TimerTask(){
            public void run() {
                System.out.println("initial timer");
                //no need to kill anything because nothing is up and running
                System.exit(0);
            }
        };
        timer = new Timer();
        timer.schedule(tt,500000, 500000); //scheduling timer for 20s
        ss = new ServerSocket(port, debugLevel);
        System.out.println("Server Running");
    }
    /**
     * The main server method that accepts connections and starts off a new thread
     * to handle each accepted connection.
     */
    private synchronized void runServer() {
        Socket client;
        try {
            while (true) {
                client = ss.accept();
                names.put(client, "anonymous");
                new ServerConnection(client, this, debugLevel).start();
            }
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }


    public static void main(String args[]) {
        if (args.length < 1 || args.length > 4) {
            System.out.println("Usage: java ChatServer -p <port#> -d <debug-level>"); // local host
            System.exit(1);
        }
        try {
            ChatServer server = new ChatServer(Integer.parseInt(args[1]), Integer.parseInt(args[3]));
            server.runServer();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }
}

class ServerConnection extends Thread
{
    private Logger logger;
    private Socket client;
    private ChatServer waiter;
    private Boolean inServer = false;

    ServerConnection(Socket client, ChatServer server, int loglevel){
        this.logger = Logger.getLogger("SERVER CONNECTION LOG " + client.toString());
        if (loglevel == 0) {
            this.logger.setLevel(Level.WARNING);
        }
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
            logger.log(Level.INFO,"Connection with client " + client.getInetAddress().getHostAddress());
            out.writeObject(new Message("Contacting Server")); //if you remove this line, the output stream will fail ahead.

            while (true) {
                handleClientObject(in.readObject(), client);
                waiter.tt = new TimerTask(){
                    public void run() {
                        System.out.println("waiting for command timer");
                        for (ObjectOutputStream out : waiter.clients.values()) {
                            try {
                                out.writeObject(new Message("ServerTimeout"));
                                out.flush();
                                Thread.sleep(2000);

                            }catch (IOException | InterruptedException e){
                                System.out.println(e);

                            }
                        }
                        System.exit(0);
                    }
                };
                waiter.timer.cancel();
                waiter.timer = new Timer();
                waiter.timer.schedule(waiter.tt,20000, 20000);
            }
        } catch (EOFException e) { // Normal EOF
            try {
                client.close();
            } catch (IOException err) {
                logger.log(Level.SEVERE, err.toString());
            }
        } catch (IOException err) {
            logger.log(Level.SEVERE,"I/O error " + err); // I/O error
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.toString()); // Unknown type of request object
        }
    }

//    private String getRoom(Socket client){
//        int index=0;
//        for (Object mapElement: waiter.rooms.keySet()){
//            LinkedList thing = (LinkedList) waiter.rooms.get(mapElement);
//            for (Object potentialClient: thing) {
//                if (potentialClient==client){
//                    logger.log(Level.INFO,"CLIENT IN A ROOM");
//                    return (String) waiter.rooms.get(index);
//                }
//                index++;
//            }
//        }
//        logger.log(Level.WARNING,("CLIENT WAS NOT ABLE TO BE LOCATED IN A ROOM");
//        return "Client not in room";
//    }

    private boolean inRoom(Socket client){
        for (Object key: waiter.rooms.keySet()){
            LinkedList thing = waiter.rooms.get(key);
            for (Object s: thing) {
                if (s==client){
                    logger.log(Level.INFO,"CLIENT IN A ROOM");
                    return true;
                }
            }
        }
        return false;
    }

    private String getClientNick(Socket client){
        for (Object key: waiter.names.keySet()) {
            Socket found_client = (Socket)(key);
            if (found_client == client) {
                logger.log(Level.INFO,"IN getClientNick(); GOT NICKNAME " + key);
                return waiter.names.get(key);
            }
        }
        logger.log(Level.INFO,"IN getClientNick(); DIDN'T FIND NICKNAME" );
        return "Anonymous";
    }

    private void handleClientObject(Object obj, Socket client){
        logger.log(Level.INFO,"GOT AN OBJECT FROM: " + client);
        try {
            if (obj instanceof Message) { // got message from client
                logger.log(Level.INFO,"GOT A MESSAGE FROM: " + client);
                Message mess = ((Message) obj);
                boolean roomCheck = inRoom(client); // could be simplified with clientRoom

                if (roomCheck) {
                    LinkedList list = waiter.rooms.get(waiter.clientRoom.get(client));

                    // send message to all clients in the client's room
                    String name = getClientNick(client);

                    for (int i = 0; i < list.size(); i++) {
                        if (client != list.get(i)) {
                            ObjectOutputStream out = waiter.clients.get(list.get(i));
                            out.writeObject(new Message(name + ": "+mess.getString()));
                            out.flush();
                        }
                    }
                }
                else{
                    ObjectOutputStream out = waiter.clients.get(client);
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
                logger.log(Level.INFO,"GOT AN IRC COMMAND FROM " + client);
                ObjectOutputStream out = waiter.clients.get(client);
                IRC command = ((IRC) obj);  // got command from client
                logger.log(Level.INFO,"COMMAND " + command.command);

                if (command.command.contains("/serverTimeout")){
                    //TODO ADD LOGGER STATEMENTS
                    System.out.println("SERVERTIMEOUT KILLING THE THING");
                    Message m;
                    m = new Message("ServerTimeout");
                    out.writeObject(m);
                    out.flush();
                    try {
                        currentThread().sleep(1000);
                    }
                    catch(InterruptedException e){
                        System.out.println(e);
                    }
                }
                if (command.command.contains("/join")){
                    // or with addition of clientRoom you could just see if the client exists in the list
                    boolean in = inRoom(client);
                    if (!in) { // make sure the client is only in one room.
                        Message m;
                        String room = command.command.substring(5);
                        if (room.trim().length()==0){ //no room name given
                            logger.log(Level.INFO,"CLIENT ATTEMPTED TO JOIN ROOM WITHOUT SPECIFYING A NAME");
                            m = new Message("YOU MUST ENTER THE NAME OF THE ROOM YOU WISH TO JOIN (Ex. /join music room)");
                            out.writeObject(m);
                            out.flush();
                            return;
                        }
                        else if (waiter.rooms.get(room) == null) { // make room if it doesnt exist
                            waiter.rooms.put(room, new LinkedList<>());
                            m = new Message("Room" + room + " was created");
                            waiter.clientRoom.put(client, room);
                        } else {
                            m = new Message("You have joined " + room);
                            waiter.clientRoom.put(client, room);
                        }

                        // reformat the rooms list
                        LinkedList<Socket> list = waiter.rooms.get(room);
                        list.add(client);
                        waiter.rooms.put(room, list);
                        out.writeObject(m);
                        out.flush();
                    }
                    else{
                        logger.log(Level.INFO,"CLIENT ATTEMPT TO ENTER MULTIPLE ROOMS");
                        out.writeObject(new Message("YOU MUST EXIT THE CURRENT ROOM BEFORE JOINING ANOTHER"));
                        out.flush();
                    }
                }
                else if (command.command.contains("/leave")) {
                    Message m;
                    String room = command.command.substring(6);
                    if (room.trim().length() == 0){
                        logger.log(Level.INFO,"CLIENT ATTEMPTED TO LEAVE ROOM WITHOUT SPECIFYING A NAME");
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
                        logger.log(Level.INFO,"Client " + client + " Leaving room " + room);
                        LinkedList <Socket> list = waiter.rooms.get(room);
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
                    logger.log(Level.INFO,"LISTING ROOMS FOR " + client);
                    String str = "Available Rooms:\n";
                    str += waiter.rooms.keySet().toString().replace("[", "").replace("]", "");
                    Message m = new Message(str);
                    out.writeObject(m);
                    out.flush();
                }
                else if (command.command.contains("/connect")) {
                    if(!inServer){
                        logger.log(Level.INFO,"CLIENT ATTEMPTING A CONNECT " + client);
                        out.writeObject(new Message("YOU HAVE CONNECTED TO SERVER"));
                        inServer = true;

                    }
                    else{ //IM A FEATRUE NOT A BUG
                        logger.log(Level.INFO,"CLIENT ATTEMPTING A RECONNECT " + client);
                        out.writeObject(new Message("CONNECTED!"));
                    }
                    out.flush();
                }
                else if (command.command.contains("/nick")){
                    logger.log(Level.INFO,"CLIENT ATTEMPTING TO ADD/ALTER NICKNAME " + client);
                    String name = command.command.substring(5);
                    if (!waiter.names.containsValue(name)) { // if name doesn't already exist
                        logger.log(Level.INFO,"CLIENT " + client + " ADDED THE NICKNAME " + name);
                        waiter.names.put(client, name);
                        out.writeObject(new Message("Great to see you," + name + ". Your nickname has been added." ));
                        out.flush();
                    }else{
                        logger.log(Level.INFO,"CLIENT " + client + " NICKNAME ALREADY TAKEN " + name);
                        out.writeObject(new Message("Sorry"+ name + " is already taken. Please choose another nickname"));
                        out.flush();
                    }
                }
                else if (command.command.contains("/quit")) {
                    waiter.names.remove(client);
                    logger.log(Level.INFO,"CLIENT REMOVED FROM NAMES");
                    //TODO remove client from room
//                    String room = getRoom(client);
                    if(inRoom(client)) { //If client in room
                        String room = waiter.clientRoom.get(client);
                        waiter.clientRoom.remove(client);
                        logger.log(Level.INFO,"ROOM BEFORE ATTEMPTING REMOVE " + waiter.rooms.get(room));
                        LinkedList<Socket> roomList = waiter.rooms.get(room);
                        roomList.remove(client);
                        logger.log(Level.INFO,"REMOVED CLIENT FROM ROOM");
                        logger.log(Level.INFO,"ROOM AFTER ATTEMPTING REMOVE " + waiter.rooms.get(room));
                        waiter.rooms.put(room, roomList);
                    }
                    out.writeObject(new Message("YOU HAVE SUCCESSFULLY QUIT"));
                    out.flush();
                    //disconnect socket
//                    client.close();
                    logger.log(Level.INFO,"CLIENT QUITING " + client);
                }
                else if (command.command.contains("/stats")) {
                    String sb = "===============================\n";
                    sb += "STATS:\n===============================";
                    sb += "\nNUM OF CLIENTS " + waiter.clients.size();
                    sb += "\nNUM OF ROOMS " + waiter.rooms.size();
                    sb += "\nCLIENTS CONNECTED " + waiter.names.values();
                    sb += "\nWHO IS IN WHAT ROOMS " + waiter.clientRoom.toString();

//                    for (Object key: waiter.names.keySet()) {
//                        sb += (String) key;
//                    }
                    out.writeObject(new Message(sb));
                    out.flush();
                    logger.log(Level.INFO,"WROTE STATS TO CLIENT " + client);

                    //TODO print off len list in rooms
                    //num of clients connected
                    //potentially timer stats
                    //maybe counter for num of times messages received
                }
                else{
                    logger.log(Level.WARNING,"CLIENT " + client + " GAVE A BAD COMMAND " + "\""+command.command+"\"");
                }
            }
            else{
                logger.log(Level.WARNING,"OBJECT FROM "+ client+ " WAS NOT MESSAGE or IRC COMMAND. OBJECT WAS: " + obj.getClass());
            }
        }catch (IOException e){
            logger.log(Level.SEVERE,"HOLY HELL THAT SHOULDN'T HAVE HAPPENED" + e );
        }
    }
}


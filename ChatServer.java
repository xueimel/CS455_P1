import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.*;

public class ChatServer {
    public Map<Socket, String> names; // Nickname: IP...or reverse
    public Map<String, LinkedList<Socket>> rooms; // Room: [participants]
    public Map<Socket, ObjectOutputStream> clients;
    public Map<Socket, String> clientRoom;
    private int debugLevel;
    private static ServerSocket ss;
    public TimerTask tt;
    public Timer timer;

    private ChatServer(int port, int debugLevel) throws IOException {
        Logger serverLog = Logger.getLogger("SERVER LOG");
        this.debugLevel = debugLevel;
        names = Collections.synchronizedMap(new HashMap<>());
        rooms = Collections.synchronizedMap(new HashMap<>());
        clients = Collections.synchronizedMap(new HashMap<>());
        clientRoom = Collections.synchronizedMap(new HashMap<>());


        tt = new TimerTask() {
            public void run() {
                serverLog.log(Level.INFO, "TIMER INITIALIZED; NO CLIENTS YET");
                System.exit(0);
            }
        };
        timer = new Timer();
        timer.schedule(tt, 500000, 500000); //scheduling timer for 5 min
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

class ServerConnection extends Thread {
    private Logger logger;
    private Socket client;
    private ChatServer waiter;
    private Boolean inServer = false;

    ServerConnection(Socket client, ChatServer server, int loglevel) {
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
            logger.log(Level.INFO, "Connection with client " + client.getInetAddress().getHostAddress());
            out.writeObject(new Message("Attempting to create connection...")); //if you remove this line, the output stream will fail ahead.

            while (true) {
                maintain(in);
            }
        } catch (EOFException e) { // Normal EOF
            try {
                client.close();
            } catch (IOException err) {
                logger.log(Level.SEVERE, err.toString());
            }
        } catch (IOException err) {
            logger.log(Level.SEVERE, "I/O error " + err); // I/O error
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.toString()); // Unknown type of request object
        }
    }

    private void maintain(ObjectInputStream in) throws ClassNotFoundException, IOException, ClassNotFoundException{
        handleClientObject(in.readObject(), client);

        waiter.tt = new TimerTask() {
            public void run() {
                logger.log(Level.SEVERE, "TIMEOUT OCCURRED CLOSING CLIENTS");
                for (ObjectOutputStream out : waiter.clients.values()) {
                    try {
                        out.writeObject(new Message("ServerTimeout"));
                        out.flush();
                        Thread.sleep(1000);
                        out.close();
                    } catch (IOException | InterruptedException e) {
                        System.out.println(e);
                    }
                }
                logger.log(Level.SEVERE, "EXITING SERVER");
                System.exit(0);
            }
        };
        waiter.timer.cancel();
        waiter.timer = new Timer();
        waiter.timer.schedule(waiter.tt, 50000, 50000);
    }

    private boolean inRoom(Socket client) {
        for (Object key : waiter.rooms.keySet()) {
            LinkedList thing = waiter.rooms.get(key);
            for (Object s : thing) {
                if (s == client) {
                    logger.log(Level.INFO, "CLIENT IN A ROOM");
                    return true;
                }
            }
        }
        return false;
    }

    private String getClientNick(Socket client) {
        for (Object key : waiter.names.keySet()) {
            Socket found_client = (Socket) (key);
            if (found_client == client) {
                logger.log(Level.INFO, "IN getClientNick(); GOT NICKNAME " + key);
                return waiter.names.get(key);
            }
        }
        logger.log(Level.INFO, "IN getClientNick(); DIDN'T FIND NICKNAME");
        return "Anonymous";
    }

    private Message join(IRC command) {
        // or with addition of clientRoom you could just see if the client exists in the list
        boolean in = inRoom(client);
        Message m;
        if (!in) { // make sure the client is only in one room.
            String room = command.command.substring(5);
            if (room.trim().length() == 0) { //no room name given
                logger.log(Level.INFO, "CLIENT ATTEMPTED TO JOIN ROOM WITHOUT SPECIFYING A NAME");
                m = new Message("YOU MUST ENTER THE NAME OF THE ROOM YOU WISH TO JOIN (Ex. /join music room)");
                return m;
            } else if (waiter.rooms.get(room) == null) { // make room if it doesnt exist
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
            return m;
        } else {
            logger.log(Level.INFO, "CLIENT ATTEMPT TO ENTER MULTIPLE ROOMS");
            m = new Message("YOU MUST EXIT THE CURRENT ROOM BEFORE JOINING ANOTHER");
            return m;
        }
    }

    private Message leave(IRC command) {
        String room = command.command.substring(6);
        if (room.trim().length() == 0) {
            logger.log(Level.INFO, "CLIENT ATTEMPTED TO LEAVE ROOM WITHOUT SPECIFYING A NAME");
            return new Message("YOU MUST ENTER THE NAME OF THE ROOM YOU WISH TO LEAVE (Ex. /leave music room)");
        }
        boolean inTheRoom = false;
        for (Object mapElement : waiter.rooms.keySet()) { // make sure client in the given room
            if (mapElement.toString().equals(room)) {
                inTheRoom = true;
            }
        }
        if (inTheRoom) {
            logger.log(Level.INFO, "Client " + client + " Leaving room " + room);
            LinkedList<Socket> list = waiter.rooms.get(room);
            list.remove(client);
            waiter.rooms.put(room, list);
            waiter.clientRoom.remove(client);
            return new Message("YOU HAVE SUCCESSFULLY LEFT ROOM" + room);
        } else {
            return new Message("YOU ARE NOT CURRENTLY IN THE ROOM YOU SPECIFIED");

        }
    }

    private Message list() {
        String str = "\n===============================\n";
        str += "ROOMS:\n";
        logger.log(Level.INFO, "LISTING ROOMS FOR " + client);
        if (waiter.rooms.size() != 0) {
            for (String room : waiter.rooms.keySet()) {
                str += room.replace("[", "").replace("]", "") + ':';
                str += " Number of clients " + waiter.rooms.get(room).size() + '\n';
            }
        } else {
            str += "NO OPEN ROOMS YET";
        }
        return new Message(str);
    }


    private Message connect() {
        if (!inServer) {
            logger.log(Level.INFO, "CLIENT ATTEMPTING A CONNECT " + client);
            inServer = true;
            return new Message("YOU HAVE CONNECTED TO SERVER");

        } else {
            logger.log(Level.INFO, "CLIENT ATTEMPTING A SECOND CONNECTION " + client);
            return new Message("YOU ARE ALREADY CONNECTED");
        }
    }


    private Message nick(IRC command) {
        logger.log(Level.INFO, "CLIENT ATTEMPTING TO ADD/ALTER NICKNAME " + client);
        String name = command.command.substring(5);
        if (!waiter.names.containsValue(name)) { // if name doesn't already exist
            logger.log(Level.INFO, "CLIENT " + client + " ADDED THE NICKNAME " + name);
            waiter.names.put(client, name);
            return new Message("Great to see you," + name + ". Your nickname has been added.");
        } else {
            logger.log(Level.INFO, "CLIENT " + client + " NICKNAME ALREADY TAKEN " + name);
            return new Message("Sorry" + name + " is already taken. Please choose another nickname");
        }
    }


    private Message quit() {
        waiter.names.remove(client);
        logger.log(Level.INFO, "CLIENT REMOVED FROM NAMES");
        if (inRoom(client)) { //If client in room
            String room = waiter.clientRoom.get(client);
            waiter.clientRoom.remove(client);
            logger.log(Level.INFO, "ROOM BEFORE ATTEMPTING REMOVE " + waiter.rooms.get(room));
            LinkedList<Socket> roomList = waiter.rooms.get(room);
            roomList.remove(client);
            logger.log(Level.INFO, "REMOVED CLIENT FROM ROOM");
            logger.log(Level.INFO, "ROOM AFTER ATTEMPTING REMOVE " + waiter.rooms.get(room));
            waiter.rooms.put(room, roomList);
        }
        return new Message("YOU HAVE SUCCESSFULLY QUIT");

    }


    private Message stats() {
        logger.log(Level.INFO, "WROTE STATS TO CLIENT " + client);
        String str = "\n===============================\n";
        str += "STATS:\n===============================";
        str += "\nNUM OF CLIENTS " + waiter.clients.size();
        str += "\nNUM OF ROOMS " + waiter.rooms.size();
        str += "\nCLIENTS CONNECTED " + waiter.names.values().toString().replace("]", "").replace("[", "");
        str += "\nWHO IS WHERE " + waiter.clientRoom.toString();
        return new Message(str);
    }


    private Message help() {
        String str = "\n===============================\n";
        str += "USAGE:\n===============================";
        str += "\n<server-name> -> Connect to named server";
        str += "\nnick <nickname> -> Pick a nickname (should be unique among active users)";
        str += "\n/list -> List channels and number of users";
        str += "\n/join <channel> -> Join a channel, all text typed is sent to all users on the channel";
        str += "\n/leave [<channel>] -> Leave the current (or named) channel";
        str += "\n/quit -> Leave chat and disconnect from server";
        str += "\n/help -> Print out help message/statsAsk server for some stats";
        return new Message(str);
    }


    private Message timeout() {
        logger.log(Level.SEVERE, "SERVER TIMEOUT. KILLING SERVER NOW.");
        return new Message("ServerTimeout");

    }


    private void handleClientObject(Object obj, Socket client) {
        logger.log(Level.INFO, "GOT AN OBJECT FROM: " + client);
        try {
            ObjectOutputStream out = waiter.clients.get(client);

            if (obj instanceof IRC) {
                logger.log(Level.INFO, "GOT AN IRC COMMAND FROM " + client);
                IRC command = ((IRC) obj);  // got command from client
                logger.log(Level.INFO, "COMMAND " + command.command);

                Message m;
                if (command.command.contains("/join")) {
                    m = join(command);
                } else if (command.command.contains("/leave")) {
                    m = leave(command);
                } else if (command.command.contains("/list")) {
                    m = list();
                } else if (command.command.contains("/connect")) {
                    m = connect();
                } else if (command.command.contains("/nick")) {
                    m = nick(command);
                } else if (command.command.contains("/quit")) {
                    logger.log(Level.INFO, "CLIENT QUITING " + client);
                    m = quit();
                    out.writeObject(m); // get confirmation back to client before cleanup
                    out.flush();
                    //disconnect socket
                    client.close();
                    return;
                } else if (command.command.contains("/stats")) {
                    m = stats();
                } else if (command.command.contains("/help")) {
                    m = help();
                } else if (command.command.contains("/serverTimeout")) {
                    m = timeout();
                    out.writeObject(m);
                    out.flush();
                    try {
                        Thread.sleep(1000); // gives clients time to cleanup
                    } catch (InterruptedException e) {
                        System.out.println("I've been interrupted");
                    }
                    return;
                } else { // wasnt a recognized command
                    logger.log(Level.WARNING, "CLIENT " + client + " GAVE A BAD COMMAND " + "\"" + command.command + "\"");
                    m = new Message("Sorry, that command was not recognized.\n" + help().getString());
                }
                out.writeObject(m);
                out.flush();

            } else if (obj instanceof Message) { // got message from client
                logger.log(Level.INFO, "GOT A MESSAGE FROM: " + client);
                Message mess = ((Message) obj);

                boolean roomCheck = inRoom(client); //ensure client is in room
                if (roomCheck) { //if they are in a room, send to all clients of that room
                    LinkedList list = waiter.rooms.get(waiter.clientRoom.get(client));
                    String name = getClientNick(client);
                    for (int i = 0; i < list.size(); i++) { //broadcast to the clients
                        if (client != list.get(i)) {
                            out = waiter.clients.get(list.get(i));
                            out.writeObject(new Message(name + ": " + mess.getString()));
                            out.flush();
                        }
                    }
                } else { // they were not in a room
                    out = waiter.clients.get(client);
                    out.writeObject(new Message("you must join a room to enter a message [/join <room name>]"));
                    out.flush();
                }

            } else { // the serialized object was not recognized.
                logger.log(Level.WARNING, "OBJECT FROM " + client + " WAS NOT MESSAGE or IRC COMMAND. OBJECT WAS: " + obj.getClass());
            }
        } catch (IOException e) { // there was likely a socket error
            logger.log(Level.SEVERE, "HOLY HELL THAT SHOULDN'T HAVE HAPPENED" + e);
        }
    }
}


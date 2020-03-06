import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;


public class Client
{
    private static String host_name;
    Scanner scan;
    int port;


    public Client(String host_name, int port) throws InterruptedException{
        this.port = port;

        try {
            //ask client to connect --IRC
            scan= new Scanner(System.in);
            System.out.println("Use \"/connect <servername>\" command to start ChatService");
            host_name = scan.nextLine().substring(9);

            IRC request_conn = new IRC("/connect "+host_name); // Setup IRC
            // sysout ctrl-spc

            Socket connection = new Socket(host_name, port);
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

            // Send connection request
            out.writeObject(request_conn);
            out.flush();
            Object obj = in.readObject();

            if (obj instanceof Message){
                Message mess = (Message)(obj);
                System.out.println(mess.getString());
            }else{
                System.out.println("Bad Object Error. Exiting");
                System.exit(-1);
            }

            ThreadedWriter dickens = new ThreadedWriter(connection, in);
            ThreadedReader ts = new ThreadedReader();
            Thread thread = new Thread(ts);
            thread.start();
            Thread threadIn = new Thread(dickens);
            threadIn.start();

            while (true) {
                thread.sleep(500); // IDK WHY, but we need to sleep this in order to get it to work
                if (ts.hasInput()) { // TODO some of this logic could be replaced by using locks.
                    String input = ts.getInput();
                    if (input.contains("/")){
                        IRC comm = new IRC(input);
                        out.writeObject(comm);
                        out.flush();
                    }else{
                        Message mess = new Message(ts.getInput());
                        out.writeObject(mess);
                        out.flush();
                    }
                }

                if (dickens.newObject()) {// TODO some of this logic could be replaced by using locks.
                    obj = dickens.getObject();
                    if (obj instanceof Message) {
                        // Needs to handle message transition and user stdin (THREAD?)
                        Message mess = ((Message) obj);
                        System.out.println(mess.getString());
                    }
                }
//                thread.join();
//                threadIn.join();
            }
        } catch (IOException e) {
            System.out.println("I/O error " + e); // I/O error
        } catch (ClassNotFoundException e2) {
            System.out.println(e2); // Unknown type of response object
        }
    }
    public static void main(String args[]) {
        System.out.println(args.length);
        if (args.length != 2) {
            System.err.println("Usage: java Client <servername> <port#>"); // local host
            System.exit(1);
        }

        try {
            Client client = new Client(args[0], Integer.parseInt(args[1]));
            //TODO Add client.run()
        } catch (NumberFormatException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

}

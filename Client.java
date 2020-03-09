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
    Object lock = new Object();


    public Client (String host_name, int port) throws InterruptedException{
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

            ThreadedWriter writer = new ThreadedWriter(connection, in);
            ThreadedReader reader = new ThreadedReader();
            Thread thread = new Thread(reader);
            thread.start();
            Thread threadIn = new Thread(writer);
            threadIn.start();

            while (true) {
                thread.sleep(500); // IDK WHY, but we need to sleep this in order to get it to work
                if (reader.hasInput()) { // TODO some of this logic could be replaced by using locks.
                    String input = reader.getInput();
                    if (input.contains("/")){
                        IRC comm = new IRC(input);
                        if (comm.command.equals("/quit")){
                            out.writeObject(comm);
                            out.flush();
                            thread.sleep(500);
                            Message finalMess = (Message)writer.getObject();
                            System.out.println(finalMess.getString());
                            reader.kill();
                            thread.interrupt();
                            writer.kill();
                            connection.close();
                            break;
                        }
                        else{
                            out.writeObject(comm);
                            out.flush();
                        }
                    }else{
                        Message mess = new Message(reader.getInput());
                        out.writeObject(mess);
                        out.flush();
                    }
                }

                if (writer.newObject()) {// TODO some of this logic could be replaced by using locks.
                    obj = writer.getObject();
                    if (obj instanceof Message) {
                        Message mess = ((Message) obj);
                        if (mess.getString().equals("YOU HAVE SUCCESSFULLY QUIT")){
                            break;
                        }
                        if (mess.getString().equals("ServerTimeout")){
                            System.out.println("Server Timeout Occurred. Please Restart Client");
                            reader.kill();
                            thread.interrupt();
                            writer.kill();
                            connection.close();
                            break;
                        }
                        System.out.println(mess.getString());
                    }
                }
            }

            System.out.println("END");

            System.exit(0); //hack because I cant get the threadedReader to close
        } catch (IOException e) {
            System.out.println("Server does not exist or could not connect" + e); // I/O error
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

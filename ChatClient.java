import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;


public class ChatClient
{
    private int port;

    private ChatClient(int port) {
        this.port = port;
    }

    private int run() throws InterruptedException{
        try {
            Scanner scan = new Scanner(System.in);
            //ask client to connect --IRC
            System.out.println("Use \"/connect <servername>\" command to start ChatService");

            String host_name = scan.nextLine().substring(9);

            IRC request_conn = new IRC("/connect " + host_name); // Setup IRC

            Socket connection = new Socket(host_name, port);
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(connection.getInputStream());

            // Send connection request
            out.writeObject(request_conn);
            out.flush();
            Object obj = in.readObject();

            if (obj instanceof Message) {
                Message mess = (Message) (obj);
                System.out.println(mess.getString());
            } else {
                System.out.println("Bad Object Error. Exiting");
                System.exit(-1);
            }

            ThreadedWriter writer = new ThreadedWriter(connection, in);
            ThreadedReader reader = new ThreadedReader();
            reader.setScanner(scan);
            Thread thread = new Thread(reader);
            thread.start();
            Thread threadIn = new Thread(writer);
            threadIn.start();

            while (true) {
                    Thread.sleep(500);
                    if (reader.hasInput()) { // there is new user input
                        String input = reader.getInput();
                        if (input.contains("/")) {
                            IRC comm = new IRC(input);
                            if (comm.command.equals("/quit")) {
                                out.writeObject(comm);
                                out.flush();
                                System.out.println("Quiting ChatClient");
                                return 0;
                            } else {
                                out.writeObject(comm);
                                out.flush();
                            }
                        } else {
                            Message mess = new Message(reader.getInput());
                            out.writeObject(mess);
                            out.flush();
                        }
                    }

                    if (writer.newObject()) {// There is new server input
                        obj = writer.getObject();
                        if (obj instanceof Message) {
                            Message mess = ((Message) obj);
                            if (mess.getString().equals("YOU HAVE SUCCESSFULLY QUIT")) {
                                reader.kill();
                                writer.kill();
                                connection.close();
                                return 0;
                            }
                            if (mess.getString().equals("ServerTimeout")) {
                                reader.kill();
                                writer.kill();
                                System.out.println("The Server has closed. PLEASE PRESS ENTER TO CLEAR THE CONSOLE BEFORE RESPONDING TO THE FOLLOWING.");
                                connection.close();
                                return 0;
                            }
                            System.out.println(mess.getString());
                        }
                    }
                }
        } catch (IOException e){
            System.out.println("Server does not exist or could not connect. Please retry"); // I/O error
            return 0;
        } catch (StringIndexOutOfBoundsException e){
            System.out.println("\"Usage: java ChatClient <servername> <port#>\"\n Please Retry");
            return 0;
        } catch (ClassNotFoundException e2) {
            System.out.println("Serialization Error. Please retry "); // Unknown type of response object
            return 0;
        } catch (InterruptedException e){
            System.out.println("THREAD BROKEN");
        }
        return 0;
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("Usage: java ChatClient <port#>"); // local host
            System.exit(1);
        }

        try {
            Scanner darkly = new Scanner(System.in);
            ChatClient client = new ChatClient(Integer.parseInt(args[0]));
            int ret_val = client.run();

            if (ret_val == 0) { // allow client to rerun if not odd return value
                String val = "yes";
                while (val.equals("yes") && ret_val == 0) {
                    System.out.println("Would you like to restart the client? yes or no");
                    val = darkly.nextLine();

                    if (val.equals("yes")) {
                        client = new ChatClient(Integer.parseInt(args[0]));
                        ret_val = client.run();
                    } else { //they said anything other than "yes"
                        System.out.println("Exiting client.");
                        System.exit(0);
                    }
                }
            }
            darkly.close();
            System.exit(0);
        } catch (NumberFormatException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

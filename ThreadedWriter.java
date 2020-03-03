import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;


public class ThreadedWriter extends Thread {
    private Socket sock;
    private Object object;
    private ObjectInputStream in;
    private boolean newObject = false;

    public ThreadedWriter(Socket conn, ObjectInputStream in){
//        try {
        this.sock = conn;
        this.in = in;
//            in = new ObjectInputStream(conn.getInputStream());

    }
    public void run() {
        while(true) {
            try {
                this.object = in.readObject();
                newObject = true;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println('b');
            }
        }
    }
    public boolean newObject(){
        return newObject;
    }

    public Object getObject(){
        newObject = false;
        return object;
    }
}



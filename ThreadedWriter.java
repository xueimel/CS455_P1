import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;


public class ThreadedWriter extends Thread {
    private Socket sock;
    private Object object;
    private ObjectInputStream in;
    private boolean newObject = false;
    private boolean killed = false;


    public ThreadedWriter(Socket conn, ObjectInputStream in){
//        try {
        setPriority(NORM_PRIORITY - 1);
        this.sock = conn;
        this.in = in;
//            in = new ObjectInputStream(conn.getInputStream());

    }
    public void run() {
        while(!killed && !Thread.currentThread().isInterrupted()) {
            try {
                this.object = in.readObject();
                newObject = true;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(e);
            }
        }
//        System.out.println("EXITED WRITER");
    }
    public boolean newObject(){
        return newObject;
    }
    public void kill(){
        killed = true;

    }
    public Object getObject(){
        newObject = false;
        return object;
    }
}



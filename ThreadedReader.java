
import java.util.Scanner;

public class ThreadedReader extends Thread{
    private Scanner darkly = new Scanner(System.in);
    private String ret_val = "";
    private boolean input = false;
    private boolean killed = false;

    @Override
    public void run() {
        while(!killed && !Thread.currentThread().isInterrupted()) {
            if (darkly.hasNextLine() && !killed) {
                input = true;
                ret_val = darkly.nextLine();
            }
        }
//        System.out.println("EXITED READER");
    }
    public String getInput(){
        input = false;
        return ret_val;
    }

    public void kill(){
        killed=true;
//        darkly.close();
    }
    public boolean hasInput(){
        return input;
    }
}

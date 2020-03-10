
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ThreadedReader extends Thread{
    private Scanner darkly;
    private String ret_val = "";
    private boolean input = false;
    private boolean killed = false;

    public void setScanner(Scanner scan){
        this.darkly = scan;
    }
    @Override
    public void run() {
        try {
            while (!killed && !Thread.currentThread().isInterrupted()) {
                if (darkly.hasNextLine() && !killed) {
                    input = true;
                    ret_val = darkly.nextLine();
                    if (ret_val.equals("/quit")){
                        kill();
                        return;
                    }
                }
            }
        }catch (NoSuchElementException e){

        }
    }
    public  String getInput(){
        input = false;
        return ret_val;
    }

    public  void kill(){
        killed=true;
    }
    public boolean hasInput(){
        return input;
    }
}

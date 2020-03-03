
import java.util.Scanner;

public class ThreadedReader implements Runnable{
    private Scanner darkly = new Scanner(System.in);
    private String ret_val = "";
    private boolean input = false;

    @Override
    public void run() {
        while(true) {
            if (darkly.hasNextLine()) {
                input = true;
                darkly.nextLine();
            }
        }
    }
    public String getInput(){
        input = false;
        return ret_val;
    }
    public boolean hasInput(){
        return input;
    }
}

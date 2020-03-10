import java.io.Serializable;

public class IRC extends Object implements Serializable {
    public String command;

    public IRC(String command){
        this.command = command;
    }
}
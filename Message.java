import java.io.Serializable;

public class Message extends Object implements Serializable {
    private String message;
    // Name IN here??

    public Message(String message){
        this.message = message;
    }
    public String getString(){
        return this.message;
    }
}

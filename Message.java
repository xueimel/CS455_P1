import java.io.Serializable;

public class Message extends Object implements Serializable {
    private String message;

    public Message(String message){
        this.message = message;
    }
    public String getString(){
        return this.message;
    }
}

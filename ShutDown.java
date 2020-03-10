public class ShutDown extends Thread{
    ServerConnection sc;
    public ShutDown(){
        this.sc = null;
    }
    public ShutDown(ServerConnection sc){
        this.sc = sc;
    }
    @Override
    public void run(){
        if (sc!=null){
            System.out.println("NOTIFYING CLIENTS SYSTEM IS SHUTTING DOWN");
            sc.shutDownHook();
        }else {
            System.out.println("No one to notify, shutting down");
        }

    }
}

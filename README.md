Project number and title, team members, course number and title, semester and year.
– A file/folder manifest to guide reading through the code.
– A section on building and running the server/clients.
– A section on how you tested it.
– A section on observations/reflection on your development process and the roles of each team
members.

P1 - ChatServer
CS 455 Distributed Systems
Spring 2020
Hailee Kiesecker
Landon Lemieux
-----------------

Guide to Reading Through Code:
------------------------------------------
Server.java :
  public ChatServer(int port, int debugLevel) - houses a TimerTask, initalizes needed hashMaps.
  private synchronized void runServer() - opens a socket connection to the server, accepts clients.
  ServerConnection(Socket client, ChatServer server, int loglevel) - sets server/client, sets debug level and priority.
  public void run() - intitalizes input and output stream, houses a TimerTask for 5 minute idiel, calls handleClientObject.
  private boolean inRoom(Socket client) - tells the server if a client is in a room.
  private String getClientNick(Socket client) - sees if the client has set a nickname
  private void handleClientObject(Object obj, Socket client) - handles the message or IRC object
  
  //additional methods here after refactor ---LANDON

Client.java :
  public Client (String host_name, int port) - sets up connection to server handles if client is inputing a message or IRC

Objects:
  IRC.java:
    public IRC(String command) - sets command.
  ThreadReader.java:
     public void run() - while the client is still running read over the next input.
     public String getInput() - gets the users input.
     public void kill() - kills the running process if called.
     public boolean hasInput() - returns true if input exists.
  ThreadWriter.java:
    public ThreadedWriter(Socket conn, ObjectInputStream in) - sets priority of writer, constructor
    public void run() - while client is running sets input/object
    public boolean newObject() - returns true if given input
    public void kill() - terminates writer
    public Object getObject() - returns clients input
  Message.java:
    public Message(String message) - sets message.
    public String getString() - returns message as string.


How to Build:
------------------------------------------
You can build the Chat Server using the following commands:
terminal 01:
javac *.java
java ChatServer -p 5005 -d 1

terminal 02:
java Client localhost 5005

after which you can play with the ChatServer using the 
following commands in terminal 02:
/connect localhost
/join testRoom
"hello world"
/help
/stats
/leave or /quit


Testing:
------------------------------------------
Throughout the process of creating this ChatServer application
we were able to test new features as we went by staring up the sever
and trying things out. For example: if we had just implemented the 
/connect IRC command to test it we would launch the server and 
try connecting to it with the client side. If it was nonfunctional 
we would use our machines debugging programs or read back through 
our code/documentation. Once it was working for one of us, we would then
push our changes to our git repository and ask the other to also
test out the functionality to be sure it could work outside of our local machine.

If code did not work outside of our local machine, we would troubleshoot together
on why that might be happening and change around our implementation.



Observations and Reflection:
------------------------------------------

Looking back at our code once all of our logic is in place made
us realize that there could have been an easier way to implement all 
of the IRC commands. If we had created individual methods for each 
command instead of supplying an abundance of If Else statements it 
would have made debugging considerably cleaner. Refactoring later
allowed us to clean everything up so it is easier for an outsider
to look over our code in the future.

What we also found was that the timer itself needed to be implemented
in multiple blocks of our code due to the nature of it and how we 
implemented our code. It would be beneficial to make our TimeTask 
functionality its own method in the future and if we were to continue
working on the project that would have been executed in refactoring 
session number 02.

During our coding enviers we set up multiple branches to add different
functionalities and utilized the meeting app Zoom to talk over our individual 
additions and merge our different branches together into one. This was 
a very useful tool that we will use in the future on other programing 
projects.

//does this look alright?
Landon was responsible for a lot of the logic going on within the IRC methods
along with the refactoring and fixing of the quit command. Hailee was responsible
for the implementation of the timer along with debugging/testing/cleaning
features that had already Landon created. She also implemented the shutdown hook
for a clean CTRL-C on the server.

P1 - ChatServer
CS 455 Distributed Systems
Spring 2020
Hailee Kiesecker
Landon Lemieux

-----------------
#Guide to Reading Through Code:
------------------------------------------
##Classes:

  ###Server.java :
    public ChatServer(int port, int debugLevel) - houses a TimerTask, initalizes needed hashMaps.
    private synchronized void runServer() - opens a socket connection to the server, accepts clients.
    ServerConnection(Socket client, ChatServer server, int loglevel) - sets server/client, sets debug level and priority.
    public void run() - intitalizes input and output stream, houses a TimerTask for 5 minute idiel, calls handleClientObject.
    public void shutDownHook() - closes down all clients gracefully.
    private void maintain(ObjectInputStream in)-closes clients down gracefully if sever is inactive for 5 minutes.
    private boolean inRoom(Socket client) - tells the server if a client is in a room.
    private String getClientNick(Socket client) - sees if the client has set a nickname
    private Message join(IRC command) - executes /join IRC , joins given room.
    private Message leave(IRC command) -executes /leave IRC , leaves given room.
    private Message list() - executes /list lists IRC , out rooms.
    private Message connect() - executes /connect IRC , connects to given server.
    private Message nick(IRC command) - executes /nick IRC , replaces clients current name with a new one.
    private Message quit() - executes /quit IRC , gracefully closes client.
    private Message stats() - executes /stats IRC , shows the client different stats.
    private Message help() - executes /help IRC , shows the client usage statements. 
    private Message timeout() - logs if there is a server timeout.
    private void handleClientObject(Object obj, Socket client) - handles the message or IRC object

  ###ChatClient.java :
    public Client (int port) - sets up client for connection to server.
    client.run() - Starts and ends connection with client.
      Handles the logic for sending and recieving communications/commands between the client and server.
      Instatiates insatances of ThreadedReader and ThreadedWriter in order to facilitate constant/continuous communications.
    Main() - Launches client and facilitates 
        
   ###ShutDown.java : 
     Overloaded constructor to facilitate shutdown hooks for server running with and without clients.
     run() - calls method from the server to gracefully shutdown if clients are present. 
     
  ###ThreadReader.java: (Class for reading input from user)
     public void run() - while the client is still running, read user input.
     public String getInput() - return users input.
     public void kill() - kills the ThreadedReader process, if called.
     public boolean hasInput() - returns true if user input exists.
     
  ###ThreadWriter.java: (Class for reading from Server and writing to standard out)
     public ThreadedWriter(Socket conn, ObjectInputStream in) - sets priority of writer, constructor
     public void run() - while client is running continuously checks for server input
     public boolean newObject() - returns true if the client got object from server
     public void kill() - terminates writer loop
     public Object getObject() - returns object sent from client.

##Objects:
  ###IRC.java: (IRC Command Object)
    public IRC(String command) - String of the appropriate command.

  ###Message.java: (Message object to be passed back and forth between client and server)
    public Message(String message) - sets message field.
    public String getString() - returns message field as string.

------------------------------------------
#How to Build:
------------------------------------------
###Start the Chat Server using the following commands:
terminal 01:
```run build.xml as an ant build ``` (compile all .java files)
```java ChatServer -p 5005 -d 1``` (run the ChatServer) where ```5005``` is the port number and ```1``` is the debug level. Debug level 0 gives you all messages, where 
   debug level 1 will print only errors and a few statuses (like ```server running``` or ```server shutting down```)

terminal 02:
```java ChatClient 5005```

after which you can utilize the ChatServer using the 
following commands in terminal 02:
```/connect localhost``` you can now send messages to other clients  (if present).
```/help``` will print all IRC usage commands




##Testing:
------------------------------------------
Throughout the process of creating this ChatServer application
we incrementaly tested new features by staring up the sever
and using our client to progressively check out functionality. 
For example: if we had just implemented the  ```/connect``` IRC command to test it we would launch the server and try connecting to it with the client side. If it was nonfunctional we would use our machines debugging programs or read back through 
our code/documentation. Once it was working for one of us, we would then
push our changes to our git repository and ask the other to also
test out the functionality to be sure it could work outside of our local machine.


##Observations and Reflection:
------------------------------------------
###Landon Reflection
This project was one of the more taxing faced recently in my CS career. 
I found the openess of the implementation to be quite painful as I fell into
many pitfalls during the implementation.
Without much of a template to work with, I made several poor choices in implementation, the first of which was using a scanner, which as it turns out, is not 
thread safe. Only now when the project is over and I have exhausted my time and 
ambition, does this finally make sense.

I also struggled with the objectoutput stream in places, finding that it might 
not act the way I expected, for instance if I handed it into a method, it would
cease to be able to write.

I assume, I am not properly synchronizing my threads in my client I also found that
the only way to properly read from the ThreadReader and ThreadWriter was to
sleep the main thread for half a second. 

I think that if I had a little more guidance or asked more questions, I might have 
spent far less time yelling at my computer, but here we are anyways....

The project structure was much more than I originally anticipated, and because of
that, I found myself refactoring a fair amount of code on the server side. I would
have liked to do more, and clean the client side up as well, but time does not allow.

###Hailee Reflection
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

Landon was responsible for a lot of the logic going on within the IRC methods
along with the refactoring and fixing of the quit command. Hailee was responsible
for the implementation of the timer along with debugging/testing/cleaning
features that had already Landon created. She also implemented the shutdown hook
for a clean CTRL-C on the server.

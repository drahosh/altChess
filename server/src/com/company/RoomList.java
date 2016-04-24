package com.company;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import org.json.simple.JSONObject;
import javax.script.*;
import org.eclipse.jetty.util.thread.strategy.ExecuteProduceConsume;
import org.eclipse.jetty.websocket.api.Session;

 public  class RoomList {
    static ArrayList<Room> openRooms=new ArrayList<>();
    static ArrayList<Room> runningRooms=new ArrayList<>();
    class Player{
        String name;
        Session s;
        boolean iswhite;
        boolean inroom;
        Player(Session s,String name){
            this.s=s;
            this.name=name;
            this.inroom=false;
        }
    }
     static Random randgen=new Random();
    class Room{
        Player p1,p2;
        String gamestate;
        boolean started;
        int n_of_players;
        ScriptEngine engine;

        public  Room(){
            n_of_players=0;
            started=false;

        }
        void joinRoom(Player p){
            p.inroom=true;
            if(n_of_players==0){
                n_of_players=1;
                this.p1=p;
                return;
            }
            this.p2=p;
            this.start_running();
        }
        void start_running(){
            ScriptEngineManager factory = new ScriptEngineManager();
             engine = factory.getEngineByName("JavaScript");
            try {
                engine.eval(new java.io.FileReader("lib/helloworld.js"));
                Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                Integer l=(Integer)bindings.get("returnval");
                System.out.println(l);
               // engine.eval(new java.io.FileReader("https://ajax.googleapis.com/ajax/libs/jquery/1.12.0/jquery.min.js"));
                engine.eval(new java.io.FileReader("lib/chess.js"));
                engine.eval(new java.io.FileReader("lib/json3.js"));
               // engine.eval(new java.io.FileReader("lib/jquery-1.10.1.min.js"));

                engine.eval("game=new Chess()");

            }catch(Exception e){
                e.printStackTrace();
            }
            openRooms.remove(this);
            runningRooms.add(this);
            int coinflip=randgen.nextInt(2);
            this.gamestate="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\n";
            p1.iswhite=false;
            p2.iswhite=true;
            if(coinflip==0){
                p1.iswhite=true;
                p2.iswhite=false;
            }

        }
        boolean move(String from, String to, String promotion){
          try{engine.eval(" var move = game.move({\n" +
                  "            from: '"+(String)from+"',\n" +
                  "            to: '"+(String)to+"',\n" +
                  "            promotion: 'q' // NOTE: always promote to a queen for example simplicity\n" +
                  "        });\n" +
                  "        var returnval;    \n " +
                  "         returnval=true;\n   "+
                  "        if (move === null) returnval=false;\n");
              Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
              boolean bool=(boolean)bindings.get("returnval");
              System.out.println(bool);
              JSONObject obj = new JSONObject();
              obj.put("from",from);
              obj.put("to",to);
              obj.put("messageType","move");
              obj.put("promotion","q");
              System.out.println(obj.toJSONString());
              System.out.println("sending move");
              p1.s.getRemote().sendString(obj.toJSONString());
              p2.s.getRemote().sendString(obj.toJSONString());
          }catch(Exception e){
                e.printStackTrace();
            }
            return false;
        }


        String getJsonGameState(Player p){
            JSONObject obj = new JSONObject();
            obj.put("messageType","game");
            obj.put("fen",this.gamestate);
            obj.put("iswhite",p.iswhite);
            System.out.println(p.name+" "+p.iswhite);
            obj.put("name1",p1.name);
            obj.put("name2",p2.name);
            return obj.toJSONString();

        }

        void notifyPlayers(){
            try {
                p1.s.getRemote().sendString(this.getJsonGameState(p1));
                p2.s.getRemote().sendString(this.getJsonGameState(p2));
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }
    Room createRoom(){

        Room r=new Room();
        openRooms.add(r);
        return r;
    }
     void deleteroom(Room r){
         if(r.started){
             runningRooms.remove(r);
         }else{
             openRooms.remove(r);
         }
     }
     Player createPlayer(Session s,String name){
         return new Player(s,name);
     }

    String getJsonRoomList(){
        int openRoomsN=openRooms.size();
        ArrayList<String> playername=new ArrayList<String>();
        for(int i=0;i<openRoomsN;i++){
            playername.add(openRooms.get(i).p1.name);
        }
        JSONObject obj = new JSONObject();
        obj.put("numOfRooms",openRoomsN);
        obj.put("playernames",playername);
        obj.put("messageType","roomList");
        System.out.println(obj.toJSONString());
        return obj.toJSONString();
    }



}

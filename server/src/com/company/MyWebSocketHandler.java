package com.company;

import java.io.IOException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static java.lang.Thread.sleep;

import javax.json.Json;
import javax.json.stream.JsonParser;
@WebSocket

public class MyWebSocketHandler {
    class HelloRunnable implements Runnable {
        Session s;

        HelloRunnable(Session session) {
            this.s = session;
        }

        public void run() {
            try {
                while (true) {
                    sleep(11000);
                    s.getRemote().sendString(String.valueOf(n));

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    static int n;
    int myint;
    Session s;
    RoomList.Player p;
    RoomList r;
    RoomList.Room room;
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
        r.deleteroom(room);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());


    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());

        this.s = session;
        this.r = new RoomList();

        p = r.createPlayer(session, "testname");
        //  new Thread(new HelloRunnable(s)).start();
        //session.getRemote().
       /* try {
            session.getRemote().sendString("Hello Webbrowser");
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
    void sendNotice(String notice){
        JSONObject obj = new JSONObject();
        obj.put("messageType","notice");
        obj.put("text",notice);
        try{
        s.getRemote().sendString(obj.toJSONString());

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println(message);
        JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(message);
            if (json.get("messageType").equals( "login")) {
                p.name = (String) json.get("name");
            }
            if (json.get("messageType").equals( "getRoomList")) {
                System.out.println("sending room list to "+p.name);

                RoomList r = new RoomList();
                try {
                    s.getRemote().sendString(r.getJsonRoomList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (json.get("messageType").equals("createRoom")) {
                if (p.inroom){
                    this.sendNotice("You cant create room if youre already in one");
                    return;
                }
                this.room=r.createRoom();
                room.joinRoom(p);
                try {
                    s.getRemote().sendString(r.getJsonRoomList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (json.get("messageType").equals("joinRoom")) {
                if (p.inroom){
                    this.sendNotice("You cant join room if youre already in one");
                    return;
                }
                long l1=(long)json.get("number");
                Long l2=new Long(l1);
                room = r.openRooms.get(l2.intValue());
                room.joinRoom(p);
                room.p1.s.getRemote().sendString(room.getJsonGameState(room.p1));
                room.p2.s.getRemote().sendString(room.getJsonGameState(room.p2));
            }
            if(json.get("messageType").equals("move")){
                String from=(String)json.get("from");
                String to=(String)json.get("to");
                String promotion=(String)json.get("promotion");
                room.move(from,to,promotion);
            }


        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}

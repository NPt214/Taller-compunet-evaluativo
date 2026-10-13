package chat.server;

import chat.server.call.CallServiceI;
import chat.server.call.UdpAudioRelay;
import chat.server.chatroom.ChatRoomI;
import chat.server.privatechat.PrivateChatI;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.InitializationData;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class ServerMain {

    public static void main(String[] args) {
        int exitCode = 0;

        InitializationData initData = new InitializationData();
        initData.properties = Util.createProperties(args);
        setDefault(initData, "Ice.ThreadPool.Server.Size", "4");
        setDefault(initData, "Ice.ThreadPool.Server.SizeMax", "16");

        try (Communicator communicator = Util.initialize(args, initData)) {

            // Un solo adaptador en el puerto 10000 con dos servicios independientes
            ObjectAdapter adapter =
                    communicator.createObjectAdapterWithEndpoints(
                            "ChatAdapter",
                            "default -p 10000"
                    );


            ChatRoomI chatRoom = new ChatRoomI();
            adapter.add(chatRoom, Util.stringToIdentity("ChatService"));


            PrivateChatI privateChat = new PrivateChatI();
            adapter.add(privateChat, Util.stringToIdentity("PrivateChatService"));

            int relayPort = communicator.getProperties()
                    .getPropertyAsIntWithDefault("CallRelay.Port", 10001);
            UdpAudioRelay relay = new UdpAudioRelay(relayPort);
            relay.start();
            CallServiceI callService = new CallServiceI(chatRoom, relay);
            adapter.add(callService, Util.stringToIdentity("CallService"));
            
            chatRoom.setOnMemberLeft(callService::onRoomLeft);
            privateChat.setOnSessionEnded(nick -> {
                callService.onUserGone(nick);
                chatRoom.onConnectionLost(nick);
            });

            adapter.activate();

            System.out.println("=================================================");
            System.out.println("SERVIDOR ZEROC ICE INICIADO EXITOSAMENTE");
            System.out.println("Puerto TCP: 10000 | Endpoint: default -p 10000");
            System.out.println("Servicio chatroom : ChatService (incluye archivos)");
            System.out.println("Servicio 1 a 1    : PrivateChatService");
            System.out.println("Servicio de voz   : CallService (señalizacion Ice)");
            System.out.println("Relay de audio    : UDP " + relay.getPort());
            System.out.println("=================================================");
            System.out.println("Esperando llamadas remotas de clientes...");

            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("[ERROR SERVIDOR] Fallo critico: " + e.getMessage());
            e.printStackTrace();
            exitCode = 1;
        }

        System.exit(exitCode);
    }

    private static void setDefault(InitializationData initData, String key, String value) {
        if (initData.properties.getProperty(key).isEmpty()) {
            initData.properties.setProperty(key, value);
        }
    }
}

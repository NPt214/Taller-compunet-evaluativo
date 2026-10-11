package chat.server;

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

            ObjectAdapter adapter =
                    communicator.createObjectAdapterWithEndpoints(
                            "ChatAdapter",
                            "default -p 10000"
                    );

            ChatRoomI servant = new ChatRoomI();

            adapter.add(servant, Util.stringToIdentity("ChatService"));

            adapter.activate();

            System.out.println("=================================================");
            System.out.println("SERVIDOR ZEROC ICE INICIADO EXITOSAMENTE");
            System.out.println("Puerto TCP: 10000 | Endpoint: default -p 10000");
            System.out.println("Identidad del Servicio: ChatService");
            System.out.println("=================================================");
            System.out.println("Esperando llamadas remotas de clientes...");

            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println(
                    "[ERROR SERVIDOR] Fallo critico: " + e.getMessage()
            );

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

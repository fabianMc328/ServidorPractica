
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class servidor2025 {

    private static final Map<String, String> solicitudesArchivos = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, List<String>>> archivosCompartidosConContenido = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket socketServidor = new ServerSocket(8080);
        System.out.println("Servidor iniciado en puerto 8080...");

        while (true) {
            Socket clienteSocket = socketServidor.accept();
            ManejadorCliente manejador = new ManejadorCliente(clienteSocket, solicitudesArchivos, archivosCompartidosConContenido);
            new Thread(manejador).start();
        }
    }
}


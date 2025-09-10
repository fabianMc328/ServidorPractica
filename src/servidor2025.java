import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList; // NUEVO
import java.util.List;     // NUEVO

public class servidor2025 {
    private static final String ARCHIVO_USUARIOS = "archivo.txt";

    public static void main(String[] args) throws IOException {
        ServerSocket socketespecial = null;

        try {
            socketespecial = new ServerSocket(8080);
        } catch (IOException e) {
            System.out.println("Hubo problemas en la red");
            System.exit(1);
        }

        Socket cliente = null;
        try {
            cliente = socketespecial.accept();
        } catch (IOException e) {
            System.out.println("Hubo problemas en la conexión");
            System.exit(1);
        }

        PrintWriter escritor = null;
        try {
            escritor = new PrintWriter(cliente.getOutputStream(), true);
            BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(cliente.getInputStream()));

            String opcion = lectorSocket.readLine();

            if ("1".equals(opcion)) {
                String usuario = lectorSocket.readLine();
                String contrasena = lectorSocket.readLine();
                if (registrarUsuario(usuario, contrasena)) {
                    escritor.println("✅ Usuario registrado con éxito.");
                } else {
                    escritor.println("❌ El usuario ya existe.");
                }
            } else if ("2".equals(opcion)) {
                String usuario = lectorSocket.readLine();
                String contrasena = lectorSocket.readLine();
                if (validarLogin(usuario, contrasena)) {
                    escritor.println("✅ Bienvenido, " + usuario + "!");

                    // NUEVO: Menú para mostrar usuarios
                    escritor.println("MENU_USUARIOS"); // señal al cliente

                    String accion = lectorSocket.readLine(); // espera opción del cliente
                    if ("1".equals(accion)) {
                        List<String> usuarios = obtenerUsuarios();
                        for (String u : usuarios) {
                            escritor.println(u);
                        }
                        escritor.println("FIN_LISTA"); // final de la lista
                    }

                } else {
                    escritor.println("❌ Usuario o contraseña incorrectos.");
                }
            } else {
                escritor.println("❌ Opción no válida.");
            }

            cliente.close();
            socketespecial.close();
        } catch (Exception e) {
            System.out.println("Hubo problemas con los sockets");
            System.exit(2);
        }
    }

    public static boolean registrarUsuario(String usuario, String contrasena) {
        if (validarExistencia(usuario)) {
            return false;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            pw.println(usuario + ";" + contrasena);
            return true;
        } catch (IOException e) {
            System.out.println("Error al guardar usuario.");
            return false;
        }
    }

    public static boolean validarLogin(String usuario, String contrasena) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2 && datos[0].equals(usuario) && datos[1].equals(contrasena)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer archivo.");
        }
        return false;
    }

    public static boolean validarExistencia(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2 && datos[0].equals(usuario)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // archivo no existe
        }
        return false;
    }

    // NUEVO: Obtener usuarios
    public static List<String> obtenerUsuarios() {
        List<String> usuarios = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2) {
                    usuarios.add(datos[0]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer usuarios.");
        }
        return usuarios;
    }
}


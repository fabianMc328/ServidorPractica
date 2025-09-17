import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class servidor2025 {
    private static final String ARCHIVO_USUARIOS = "archivo.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";

    public static void main(String[] args) throws IOException {
        ServerSocket socketespecial = new ServerSocket(8080);

        while (true) {
            Socket cliente = socketespecial.accept();

            PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
            BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(cliente.getInputStream()));

            String opcion = lectorSocket.readLine();

            if ("1".equals(opcion)) {
                String usuario = lectorSocket.readLine();
                String contrasena = lectorSocket.readLine();
                if (registrarUsuario(usuario, contrasena)) {
                    escritor.println("Usuario registrado con éxito.");
                } else {
                    escritor.println("El usuario ya existe.");
                }
            } else if ("2".equals(opcion)) {
                String usuario = lectorSocket.readLine();
                String contrasena = lectorSocket.readLine();
                if (validarLogin(usuario, contrasena)) {
                    escritor.println("✅ Bienvenido, " + usuario + "!");
                    List<String> mensajesPendientes = obtenerMensajes(usuario);
                    if (!mensajesPendientes.isEmpty()) {
                        escritor.println("=== Tienes mensajes recibidos ===");
                        for (String m : mensajesPendientes) {
                            escritor.println(m);
                        }
                        escritor.println("=== Fin de mensajes ===");
                    }

                    escritor.println("MENU_OPCIONES");

                    String accion;
                    while ((accion = lectorSocket.readLine()) != null) {
                        switch (accion) {
                            case "1":
                                List<String> usuarios = obtenerUsuarios();
                                for (String u : usuarios) {
                                    escritor.println(u);
                                }
                                escritor.println("FIN_LISTA");
                                break;
                            case "2":
                                int numero = (int) (Math.random() * 10) + 1;
                                int intentos = 0;
                                escritor.println("Adivina el número del 1 al 10. Tienes 3 intentos.");

                                while (intentos < 3) {
                                    String intentoStr = lectorSocket.readLine();
                                    try {
                                        int intento = Integer.parseInt(intentoStr);
                                        if (intento == numero) {
                                            escritor.println("🎉 Adivinaste el número.");
                                            escritor.println("FIN_JUEGO");
                                            break;
                                        } else {
                                            intentos++;
                                            if (intentos >= 3) {
                                                escritor.println("😢 Se acabaron los intentos. El número era: " + numero);
                                                escritor.println("FIN_JUEGO");
                                                break;
                                            } else {
                                                if (intento < numero) {
                                                    escritor.println("El número es mayor.");
                                                } else {
                                                    escritor.println("El número es menor.");
                                                }
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        intentos++;
                                        escritor.println("Ingresa un número válido.");
                                        if (intentos >= 3) {
                                            escritor.println("😢 Se acabaron los intentos. El número era: " + numero);
                                            escritor.println("FIN_JUEGO");
                                            break;
                                        }
                                    }
                                }
                                break;
                            case "3":
                                escritor.println("Usuario destinatario:");
                                String destinatario = lectorSocket.readLine();
                                escritor.println("Escribe tu mensaje:");
                                String mensaje = lectorSocket.readLine();
                                guardarMensaje(usuario, destinatario, mensaje);
                                escritor.println("Mensaje guardado para " + destinatario);
                                break;

                            default:
                                escritor.println("Opción no válida.");
                        }
                    }
                } else {
                    escritor.println("LOGIN_ERROR");
                }
            } else if ("3".equals(opcion)) {
                escritor.println("CERRAR");
            } else {
                escritor.println("Opción no válida.");
            }

            cliente.close();
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
        }
        return false;
    }

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
        }
        return usuarios;
    }

    public static List<String> obtenerMensajes(String usuario) {
        List<String> mensajes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_MENSAJES))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 3 && datos[1].equals(usuario)) {
                    mensajes.add("De " + datos[0] + ": " + datos[2]);
                }
            }
        } catch (IOException e) {
        }
        return mensajes;
    }

    public static void guardarMensaje(String remitente, String destinatario, String mensaje) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_MENSAJES, true))) {
            pw.println(remitente + ";" + destinatario + ";" + mensaje);
        } catch (IOException e) {
            System.out.println("Error al guardar mensaje: " + e.getMessage());
        }
    }
    public static boolean eliminarMensajePorIndice(String usuario, int indice, String tipo) {
        File archivo = new File(ARCHIVO_MENSAJES);
        List<String> lineasOriginales = new ArrayList<>();
        List<String> lineasFiltradas = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineasOriginales.add(linea);
            }
        } catch (IOException e) {
            return false;
        }

        int contador = -1;
        for (String linea : lineasOriginales) {
            String[] datos = linea.split(";");
            if (datos.length == 3) {
                boolean esCandidato = false;
                if ("recibido".equalsIgnoreCase(tipo) && datos[1].equals(usuario)) {
                    esCandidato = true;
                } else if ("enviado".equalsIgnoreCase(tipo) && datos[0].equals(usuario)) {
                    esCandidato = true;
                }

                if (esCandidato) {
                    contador++;
                    if (contador == indice) { //
                        continue;
                    }
                }
            }
            lineasFiltradas.add(linea);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String l : lineasFiltradas) {
                pw.println(l);
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }



}








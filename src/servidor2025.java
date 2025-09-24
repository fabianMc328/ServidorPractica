import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class servidor2025 {
    private static final String ARCHIVO_USUARIOS = "archivo.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";


    private static final String ARCHIVO_BLOQUEOS = "bloqueados.txt";

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
                                boolean juegoTerminado = false;

                                while (!juegoTerminado && intentos < 3) {
                                    String intentoStr = lectorSocket.readLine();
                                    try {
                                        int intento = Integer.parseInt(intentoStr);
                                        intentos++;
                                        if (intento == numero) {
                                            escritor.println("🎉 Adivinaste el número.");
                                            juegoTerminado = true;
                                        } else if (intentos >= 3) {
                                            escritor.println("😢 Se acabaron los intentos. El número era: " + numero);
                                            juegoTerminado = true;
                                        } else {
                                            if (intento < numero) {
                                                escritor.println("El número es mayor.");
                                            } else {
                                                escritor.println("El número es menor.");
                                            }
                                        }
                                    } catch (NumberFormatException e) {

                                        escritor.println("Ingresa un número válido.");
                                        if (intentos >= 3) {
                                            escritor.println("😢 Se acabaron los intentos. El número era: " + numero);
                                            juegoTerminado = true;
                                        }
                                    }
                                }
                                escritor.println("FIN_JUEGO");
                                break;

                            case "3":
                                String destinatario = lectorSocket.readLine();
                                if (!validarExistencia(destinatario)) {
                                    escritor.println("NO_USUARIO");
                                    break;
                                }
                                if (estaBloqueado(destinatario, usuario)) {  // <-- nuevo método verifica bloqueo inverso
                                    escritor.println("USUARIO_BLOQUEADO");
                                    break;
                                }

                                escritor.println("OK");
                                String mensaje = lectorSocket.readLine();
                                guardarMensaje(usuario, destinatario, mensaje);
                                escritor.println("Mensaje guardado para " + destinatario);
                                break;

                            case "4":
                                String tipo = lectorSocket.readLine();
                                List<String> listaMensajes = obtenerMensajesPorTipo(usuario, tipo);

                                if (listaMensajes.isEmpty()) {
                                    escritor.println("NO_HAY_MENSAJES");
                                    break;
                                }

                                for (String m : listaMensajes) {
                                    escritor.println(m);
                                }
                                escritor.println("FIN_LISTA");

                                String numStr = lectorSocket.readLine();
                                try {
                                    int indice = Integer.parseInt(numStr);
                                    boolean eliminado = eliminarMensajePorIndice(usuario, indice, tipo);
                                    if (eliminado) {
                                        escritor.println("Mensaje eliminado correctamente.");
                                    } else {
                                        escritor.println("No se pudo eliminar el mensaje.");
                                    }
                                } catch (NumberFormatException e) {
                                    escritor.println("Número inválido.");
                                }
                                break;


                            case "5":
                                List<String> mensajesUsuario = obtenerMensajes(usuario);

                                if (mensajesUsuario.isEmpty()) {
                                    escritor.println("NO_HAY_MENSAJES");
                                } else {
                                    escritor.println("HAY_MENSAJES");

                                    int total = mensajesUsuario.size();
                                    int pagina = 1;
                                    int porPagina = 3;

                                    for (int i = 0; i < total; i++) {
                                        escritor.println(mensajesUsuario.get(i));

                                        if ((i + 1) % porPagina == 0 || i == total - 1) {
                                            escritor.println("--- Página " + pagina + " ---");

                                            if (i < total - 1) {
                                                escritor.println("MAS_PAGINAS");
                                                escritor.println("¿Quieres continuar en la siguiente página? (escribe 'siguiente') o presiona cualquier tecla para cancelar.");
                                                String respuesta = lectorSocket.readLine();
                                                if (!"siguiente".equalsIgnoreCase(respuesta)) {
                                                    break;
                                                }
                                            }

                                            pagina++;
                                        }
                                    }

                                    escritor.println("FIN_LISTA");
                                }
                                break;
                            case "7":
                                String usuarioaEliminar = usuario;
                                String confirmar = lectorSocket.readLine();
                                if ("si".equalsIgnoreCase(confirmar)) {
                                    boolean eliminado = eliminarUsuario(usuarioaEliminar);
                                    if (eliminado) {
                                        escritor.println("ELIMINADO_OK");
                                    } else {
                                        escritor.println("ELIMINADO_ERROR");
                                    }
                                    accion = "6";
                                } else {
                                    escritor.println("ELIMINADO_CANCELADO");
                                }
                                break;


                            case "6":
                                escritor.println("Cerrando sesión en el servidor...");
                                break;

                            case "8":
                                String usuarioABloquear = lectorSocket.readLine();
                                if (bloquearUsuario(usuario, usuarioABloquear)) {
                                    escritor.println("Usuario bloqueado correctamente.");
                                } else {
                                    escritor.println("Error al bloquear usuario (usuario inexistente o ya bloqueado).");
                                }
                                break;

                            case "9":
                                String usuarioADesbloquear = lectorSocket.readLine();
                                if (desbloquearUsuario(usuario, usuarioADesbloquear)) {
                                    escritor.println("Usuario desbloqueado correctamente.");
                                } else {
                                    escritor.println("Error al desbloquear usuario (usuario no estaba bloqueado).");
                                }
                                break;

                            default:
                                escritor.println("Opción no válida.");
                        }

                        if ("6".equals(accion)) break;
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
        if (validarExistencia(usuario)) return false;
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
                if (datos.length == 2 && datos[0].equals(usuario) && datos[1].equals(contrasena))
                    return true;
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
                if (datos.length == 2 && datos[0].equals(usuario)) return true;
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
                if (datos.length == 2) usuarios.add(datos[0]);
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
                if (datos.length == 3 && datos[1].equals(usuario))
                    mensajes.add("De " + datos[0] + ": " + datos[2]);
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
            while ((linea = br.readLine()) != null) lineasOriginales.add(linea);
        } catch (IOException e) {
            return false;
        }

        int contador = 1;
        for (String linea : lineasOriginales) {
            String[] datos = linea.split(";");
            if (datos.length == 3) {
                boolean esCandidato = false;
                if ("recibido".equalsIgnoreCase(tipo) && datos[1].equals(usuario))
                    esCandidato = true;
                else if ("enviado".equalsIgnoreCase(tipo) && datos[0].equals(usuario))
                    esCandidato = true;

                if (esCandidato) {
                    if (contador == indice) {
                        contador++;
                        continue;
                    }
                    contador++;
                }
            }
            lineasFiltradas.add(linea);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String l : lineasFiltradas) pw.println(l);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static List<String> obtenerMensajesPorTipo(String usuario, String tipo) {
        List<String> mensajes = new ArrayList<>();
        int contador = 1;

        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_MENSAJES))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 3) {
                    String remitente = datos[0];
                    String destinatario = datos[1];
                    String mensaje = datos[2];

                    if ("recibido".equalsIgnoreCase(tipo) && destinatario.equals(usuario)) {
                        mensajes.add(contador + ". De " + remitente + ": " + mensaje);
                        contador++;
                    } else if ("enviado".equalsIgnoreCase(tipo) && remitente.equals(usuario)) {
                        mensajes.add(contador + ". Para " + destinatario + ": " + mensaje);
                        contador++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mensajes;
    }

    public static boolean eliminarUsuario(String usuario) {
        try {
            // borrar usuario de archivo.txt
            File inputFile = new File(ARCHIVO_USUARIOS);
            File tempFile = new File("tempUsuarios.txt");
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(usuario + ";")) {
                        writer.write(line + System.lineSeparator());
                    }
                }
            }
            inputFile.delete();
            tempFile.renameTo(inputFile);

            // borrar todos los mensajes
            File inputMsgs = new File(ARCHIVO_MENSAJES);
            File tempMsgs = new File("tempMensajes.txt");
            try (BufferedReader reader = new BufferedReader(new FileReader(inputMsgs));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(tempMsgs))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";", 3);
                    if (parts.length == 3) {
                        String remitente = parts[0];
                        String destinatario = parts[1];
                        if (!remitente.equals(usuario) && !destinatario.equals(usuario)) {
                            writer.write(line + System.lineSeparator());
                        }
                    }
                }
            }
            inputMsgs.delete();
            tempMsgs.renameTo(inputMsgs);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean bloquearUsuario(String bloqueador, String bloqueado) {
        if (!validarExistencia(bloqueado) || bloqueador.equals(bloqueado)) return false;

        File archivo = new File("bloqueados.txt");


        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.equals(bloqueador + ";" + bloqueado)) {
                    return false;
                }
            }
        } catch (IOException e) {

        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
            pw.println(bloqueador + ";" + bloqueado);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean desbloquearUsuario(String bloqueador, String bloqueado) {
        File archivo = new File("bloqueados.txt");
        File tempFile = new File("tempBloqueados.txt");
        boolean encontrado = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(archivo));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.equals(bloqueador + ";" + bloqueado)) {
                    encontrado = true;
                    continue;
                }
                writer.write(linea + System.lineSeparator());
            }

        } catch (IOException e) {
            return false;
        }

        archivo.delete();
        tempFile.renameTo(archivo);
        return encontrado;
    }

    public static boolean estaBloqueado(String usuarioBloqueador, String usuarioBloqueado) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_BLOQUEOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2) {
                    if (datos[0].equals(usuarioBloqueador) && datos[1].equals(usuarioBloqueado)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
        }
        return false;
    }
}
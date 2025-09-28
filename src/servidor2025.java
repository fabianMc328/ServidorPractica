import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class servidor2025 {
    private static final String ARCHIVO_USUARIOS = "archivo.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";
    private static final String ARCHIVO_BLOQUEOS = "bloqueados.txt";

    private static final List<SolicitudArchivo> solicitudesPendientes = new ArrayList<>();
    private static final List<ArchivoCompartido> archivosCompartidos = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket socketespecial = new ServerSocket(8080);
        System.out.println("Servidor iniciado en puerto 8080...");

        while (true) {
            Socket cliente = socketespecial.accept();

            new Thread(() -> {
                try {
                    manejarCliente(cliente);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void manejarCliente(Socket cliente) throws IOException {
        PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
        BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(cliente.getInputStream()));

        String opcion = lectorSocket.readLine();

        if ("1".equals(opcion)) { // Registrar usuario
            String usuario = lectorSocket.readLine();
            String contrasena = lectorSocket.readLine();
            if (registrarUsuario(usuario, contrasena)) {
                escritor.println("Usuario registrado con éxito.");
            } else {
                escritor.println("El usuario ya existe.");
            }
        } else if ("2".equals(opcion)) { // Login
            String usuario = lectorSocket.readLine();
            String contrasena = lectorSocket.readLine();
            if (validarLogin(usuario, contrasena)) {
                escritor.println("✅ Bienvenido, " + usuario + "!");
                escritor.println("MENU_OPCIONES");

                String accion;
                while ((accion = lectorSocket.readLine()) != null) {
                    switch (accion) {
                        case "1": // Mostrar usuarios registrados
                            List<String> usuarios = obtenerUsuarios();
                            for (String u : usuarios) escritor.println(u);
                            escritor.println("FIN_LISTA");
                            break;

                        case "2": // Juego de adivinar número
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
                                        if (intento < numero) escritor.println("El número es mayor.");
                                        else escritor.println("El número es menor.");
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

                        case "3": // Enviar mensaje
                            String destinatario = lectorSocket.readLine();
                            if (!validarExistencia(destinatario)) {
                                escritor.println("NO_USUARIO");
                                break;
                            }
                            if (estaBloqueado(destinatario, usuario)) {
                                escritor.println("USUARIO_BLOQUEADO");
                                break;
                            }
                            escritor.println("OK");
                            String mensaje = lectorSocket.readLine();
                            guardarMensaje(usuario, destinatario, mensaje);
                            escritor.println("Mensaje guardado para " + destinatario);
                            break;

                        case "4": // Eliminar mensaje
                            String tipo = lectorSocket.readLine();
                            List<String> listaMensajes = obtenerMensajesPorTipo(usuario, tipo);
                            if (listaMensajes.isEmpty()) {
                                escritor.println("NO_HAY_MENSAJES");
                                break;
                            }
                            for (String m : listaMensajes) escritor.println(m);
                            escritor.println("FIN_LISTA");

                            String numStr = lectorSocket.readLine();
                            try {
                                int indice = Integer.parseInt(numStr);
                                boolean eliminado = eliminarMensajePorIndice(usuario, indice, tipo);
                                if (eliminado) escritor.println("Mensaje eliminado correctamente.");
                                else escritor.println("No se pudo eliminar el mensaje.");
                            } catch (NumberFormatException e) {
                                escritor.println("Número inválido.");
                            }
                            break;

                        case "5": // Leer mensajes con paginación
                            List<String> mensajesUsuario = obtenerMensajes(usuario);
                            if (mensajesUsuario.isEmpty()) {
                                escritor.println("NO_HAY_MENSAJES");
                                break;
                            }
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
                                        if (!"siguiente".equalsIgnoreCase(respuesta)) break;
                                    }
                                    pagina++;
                                }
                            }
                            escritor.println("FIN_LISTA");
                            break;

                        case "6": // Salir
                            escritor.println("Cerrando sesión en el servidor...");
                            return;

                        case "7": // Eliminar cuenta
                            String usuarioaEliminar = usuario;
                            String confirmar = lectorSocket.readLine();
                            if ("si".equalsIgnoreCase(confirmar)) {
                                boolean eliminado = eliminarUsuario(usuarioaEliminar);
                                if (eliminado) escritor.println("ELIMINADO_OK");
                                else escritor.println("ELIMINADO_ERROR");
                                return;
                            } else {
                                escritor.println("ELIMINADO_CANCELADO");
                            }
                            break;

                        case "8": // Bloquear usuario
                            String usuarioABloquear = lectorSocket.readLine();
                            if (bloquearUsuario(usuario, usuarioABloquear)) escritor.println("Usuario bloqueado correctamente.");
                            else escritor.println("Error al bloquear usuario (usuario inexistente o ya bloqueado).");
                            break;

                        case "9": // Desbloquear usuario
                            String usuarioADesbloquear = lectorSocket.readLine();
                            if (desbloquearUsuario(usuario, usuarioADesbloquear)) escritor.println("Usuario desbloqueado correctamente.");
                            else escritor.println("Error al desbloquear usuario (usuario no estaba bloqueado).");
                            break;

                        case "10": // Solicitar ver archivos
                            String objetivo = lectorSocket.readLine();
                            if (!validarExistencia(objetivo)) {
                                escritor.println("USUARIO_NO_EXISTE");
                                break;
                            }
                            synchronized (solicitudesPendientes) {
                                solicitudesPendientes.add(new SolicitudArchivo(usuario, objetivo));
                            }
                            escritor.println("Solicitud enviada a " + objetivo + ". Esperando respuesta cuando se conecte.");
                            break;

                        case "11": // Leer solicitudes de archivos
                            synchronized (solicitudesPendientes) {
                                List<SolicitudArchivo> pendientes = new ArrayList<>();
                                for (SolicitudArchivo s : solicitudesPendientes) {
                                    if (s.getDestino().equals(usuario)) pendientes.add(s);
                                }

                                if (pendientes.isEmpty()) escritor.println("FIN_SOLICITUDES");
                                else {
                                    for (SolicitudArchivo s : pendientes) {
                                        escritor.println("SOLICITUD_ARCHIVOS");
                                        escritor.println(s.getOrigen());
                                        String decision = lectorSocket.readLine();
                                        if ("si".equalsIgnoreCase(decision)) {
                                            escritor.println("PEDIR_LISTA_ARCHIVOS");
                                            String lineaArchivo;
                                            List<String> archivosEnviados = new ArrayList<>();
                                            while (!(lineaArchivo = lectorSocket.readLine()).equals("FIN_LISTA_ARCHIVOS")) {
                                                archivosEnviados.add(lineaArchivo);
                                            }
                                            archivosCompartidos.add(new ArchivoCompartido(s.getOrigen(), s.getDestino(), archivosEnviados));
                                            escritor.println("ARCHIVOS_COMPARTIDOS");
                                        } else {
                                            escritor.println("NO_COMPARTIDOS");
                                        }
                                        solicitudesPendientes.remove(s);
                                    }
                                    escritor.println("FIN_SOLICITUDES");
                                }
                            }
                            break;

                        case "12": // Ver archivos compartidos
                            boolean tieneArchivos = false;
                            synchronized (archivosCompartidos) {
                                for (ArchivoCompartido ac : archivosCompartidos) {
                                    if (ac.getSolicitante().equals(usuario)) {
                                        tieneArchivos = true;
                                        escritor.println("--- Archivos compartidos por " + ac.getDueño() + " ---");
                                        for (String archivo : ac.getArchivos()) {
                                            escritor.println(archivo);
                                        }
                                    }
                                }
                            }
                            if (!tieneArchivos) escritor.println("NO_HAY_ARCHIVOS");
                            escritor.println("FIN_LISTA_ARCHIVOS");
                            break;


                        default:
                            escritor.println("Opción no válida.");
                    }
                }

            } else {
                escritor.println("LOGIN_ERROR");
            }
        } else if ("3".equals(opcion)) { // Salir
            escritor.println("CERRAR");
        } else {
            escritor.println("Opción no válida.");
        }

        cliente.close();
    }

    // -------------------- Métodos auxiliares --------------------
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
        } catch (IOException e) {}
        return false;
    }

    public static boolean validarExistencia(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2 && datos[0].equals(usuario)) return true;
            }
        } catch (IOException e) {}
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
        } catch (IOException e) {}
        return usuarios;
    }

    public static void guardarMensaje(String remitente, String destinatario, String mensaje) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_MENSAJES, true))) {
            pw.println(remitente + ";" + destinatario + ";" + mensaje);
        } catch (IOException e) {
            System.out.println("Error al guardar mensaje: " + e.getMessage());
        }
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
        } catch (IOException e) {}
        return mensajes;
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
        } catch (IOException e) { e.printStackTrace(); }
        return mensajes;
    }

    public static boolean eliminarMensajePorIndice(String usuario, int indice, String tipo) {
        File archivo = new File(ARCHIVO_MENSAJES);
        List<String> lineasOriginales = new ArrayList<>();
        List<String> lineasFiltradas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) lineasOriginales.add(linea);
        } catch (IOException e) { return false; }

        int contador = 1;
        for (String linea : lineasOriginales) {
            String[] datos = linea.split(";");
            if (datos.length == 3) {
                boolean esCandidato = false;
                if ("recibido".equalsIgnoreCase(tipo) && datos[1].equals(usuario)) esCandidato = true;
                if ("enviado".equalsIgnoreCase(tipo) && datos[0].equals(usuario)) esCandidato = true;

                if (esCandidato) {
                    if (contador == indice) continue;
                    contador++;
                }
            }
            lineasFiltradas.add(linea);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String l : lineasFiltradas) pw.println(l);
            return true;
        } catch (IOException e) { return false; }
    }

    public static boolean eliminarUsuario(String usuario) {
        boolean eliminado = false;
        File archivo = new File(ARCHIVO_USUARIOS);
        List<String> lineasOriginales = new ArrayList<>();
        List<String> lineasFiltradas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) lineasOriginales.add(linea);
        } catch (IOException e) { return false; }

        for (String linea : lineasOriginales) {
            if (!linea.startsWith(usuario + ";")) lineasFiltradas.add(linea);
            else eliminado = true;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String l : lineasFiltradas) pw.println(l);
        } catch (IOException e) { return false; }

        // Eliminar mensajes
        File archivoMensajes = new File(ARCHIVO_MENSAJES);
        List<String> mensajes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivoMensajes))) {
            String linea;
            while ((linea = br.readLine()) != null) mensajes.add(linea);
        } catch (IOException e) {}
        List<String> mensajesFiltrados = new ArrayList<>();
        for (String m : mensajes) {
            String[] datos = m.split(";");
            if (!datos[0].equals(usuario) && !datos[1].equals(usuario)) mensajesFiltrados.add(m);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoMensajes))) {
            for (String m : mensajesFiltrados) pw.println(m);
        } catch (IOException e) {}

        // Eliminar bloqueos
        File archivoBloqueos = new File(ARCHIVO_BLOQUEOS);
        List<String> bloqueos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivoBloqueos))) {
            String linea;
            while ((linea = br.readLine()) != null) bloqueos.add(linea);
        } catch (IOException e) {}
        List<String> bloqueosFiltrados = new ArrayList<>();
        for (String b : bloqueos) {
            String[] datos = b.split(";");
            if (!datos[0].equals(usuario) && !datos[1].equals(usuario)) bloqueosFiltrados.add(b);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoBloqueos))) {
            for (String b : bloqueosFiltrados) pw.println(b);
        } catch (IOException e) {}

        return eliminado;
    }

    public static boolean bloquearUsuario(String usuario, String bloquear) {
        if (!validarExistencia(bloquear)) return false;
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_BLOQUEOS, true))) {
            pw.println(usuario + ";" + bloquear);
            return true;
        } catch (IOException e) { return false; }
    }

    public static boolean desbloquearUsuario(String usuario, String desbloquear) {
        boolean eliminado = false;
        File archivo = new File(ARCHIVO_BLOQUEOS);
        List<String> lineas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) lineas.add(linea);
        } catch (IOException e) {}

        List<String> filtradas = new ArrayList<>();
        for (String l : lineas) {
            String[] datos = l.split(";");
            if (datos[0].equals(usuario) && datos[1].equals(desbloquear)) eliminado = true;
            else filtradas.add(l);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String l : filtradas) pw.println(l);
        } catch (IOException e) {}

        return eliminado;
    }

    public static boolean estaBloqueado(String usuario, String remitente) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_BLOQUEOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos[0].equals(usuario) && datos[1].equals(remitente)) return true;
            }
        } catch (IOException e) {}
        return false;
    }

    // -------------------- Clases internas --------------------
    static class SolicitudArchivo {
        private final String origen;
        private final String destino;
        public SolicitudArchivo(String origen, String destino) { this.origen = origen; this.destino = destino; }
        public String getOrigen() { return origen; }
        public String getDestino() { return destino; }
    }

    static class ArchivoCompartido {
        private final String solicitante;
        private final String dueño;
        private final List<String> archivos;
        public ArchivoCompartido(String solicitante, String dueño, List<String> archivos) {
            this.solicitante = solicitante;
            this.dueño = dueño;
            this.archivos = archivos;
        }
        public String getSolicitante() { return solicitante; }
        public String getDueño() { return dueño; }
        public List<String> getArchivos() { return archivos; }
    }
}


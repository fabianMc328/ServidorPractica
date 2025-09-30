

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManejadorCliente implements Runnable {

    private final Socket clienteSocket;
    private final Map<String, String> solicitudesArchivos;
    private final Map<String, Map<String, List<String>>> archivosCompartidos;

    private PrintWriter escritor;
    private BufferedReader lector;
    private String usuario;

    public ManejadorCliente(Socket socket, Map<String, String> solicitudes, Map<String, Map<String, List<String>>> compartidos) {
        this.clienteSocket = socket;
        this.solicitudesArchivos = solicitudes;
        this.archivosCompartidos = compartidos;
    }

    @Override
    public void run() {
        try {
            this.escritor = new PrintWriter(clienteSocket.getOutputStream(), true);
            this.lector = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));

            String opcionInicial = lector.readLine();
            if ("1".equals(opcionInicial)) {
                gestionarRegistro();
            } else if ("2".equals(opcionInicial)) {
                gestionarLogin();
            }

        } catch (IOException e) {
            System.out.println("Error al manejar cliente: " + e.getMessage());
        } finally {
            try {
                clienteSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void gestionarRegistro() throws IOException {
        String nuevoUsuario = lector.readLine();
        String contrasena = lector.readLine();
        if (GestorDeDatos.registrarUsuario(nuevoUsuario, contrasena)) {
            escritor.println("Usuario registrado con éxito.");
        } else {
            escritor.println("El usuario ya existe.");
        }
    }

    private void gestionarLogin() throws IOException {
        this.usuario = lector.readLine();
        String contrasena = lector.readLine();

        if (GestorDeDatos.validarCredenciales(usuario, contrasena)) {
            escritor.println("✅ Bienvenido, " + usuario + "!");
            escritor.println("MENU_OPCIONES");
            gestionarSesionActiva();
        } else {
            escritor.println("LOGIN_ERROR");
        }
    }

    private void gestionarSesionActiva() throws IOException {
        String accion;
        while ((accion = lector.readLine()) != null) {
            switch (accion) {
                case "1": mostrarUsuarios(); break;
                case "2": iniciarJuego(); break;
                case "3": enviarMensaje(); break;
                case "4": eliminarMensaje(); break;
                case "5": leerMensajesPaginados(); break;
                case "6": return;
                case "7": eliminarCuenta(); return;
                case "8": bloquearUsuario(); break;
                case "9": desbloquearUsuario(); break;
                case "10": solicitarVerArchivos(); break;
                case "11": gestionarSolicitudDeArchivos(); break;
                case "12": verYCopiarArchivosCompartidos(); break;
                default: escritor.println("Opción no válida.");
            }
        }
    }

    private void mostrarUsuarios() {
        List<String> usuarios = GestorDeDatos.obtenerListaDeUsuarios();
        for (String u : usuarios) escritor.println(u);
        escritor.println("FIN_LISTA");
    }

    private void iniciarJuego() throws IOException {
        int numeroSecreto = (int) (Math.random() * 10) + 1;
        escritor.println("Adivina el número del 1 al 10. Tienes 3 intentos.");


        for (int intentoActual = 1; intentoActual <= 3; intentoActual++) {
            try {
                int numeroDelUsuario = Integer.parseInt(lector.readLine());


                if (numeroDelUsuario == numeroSecreto) {
                    escritor.println("🎉 Adivinaste el número.");
                    escritor.println("FIN_JUEGO");
                    return;
                }


                if (intentoActual == 3) {
                    escritor.println("😢 Se acabaron los intentos. El número era: " + numeroSecreto);
                    escritor.println("FIN_JUEGO");
                    return;
                }
                escritor.println(numeroDelUsuario < numeroSecreto ? "El número es mayor." : "El número es menor.");

            } catch (NumberFormatException e) {
                if (intentoActual == 3) {
                    escritor.println("😢 Se acabaron los intentos (inválido). El número era: " + numeroSecreto);
                    escritor.println("FIN_JUEGO");
                    return;
                }
                escritor.println("Ingresa un número válido.");
            }
        }
    }

    private void enviarMensaje() throws IOException {
        String destinatario = lector.readLine();
        if (!GestorDeDatos.existeUsuario(destinatario)) {
            escritor.println("NO_USUARIO");
            return;
        }
        if (GestorDeDatos.estaBloqueado(destinatario, this.usuario)) {
            escritor.println("USUARIO_BLOQUEADO");
            return;
        }
        escritor.println("OK");
        String mensaje = lector.readLine();
        GestorDeDatos.guardarMensaje(this.usuario, destinatario, mensaje);
        escritor.println("Mensaje guardado para " + destinatario);
    }

    private void eliminarMensaje() throws IOException {
        String tipo = lector.readLine();
        List<String> mensajes = GestorDeDatos.obtenerMensajesPorTipo(this.usuario, tipo);
        if (mensajes.isEmpty()) {
            escritor.println("NO_HAY_MENSAJES");
            return;
        }
        for (String m : mensajes) escritor.println(m);
        escritor.println("FIN_LISTA");

        try {
            int indice = Integer.parseInt(lector.readLine());
            boolean eliminado = GestorDeDatos.eliminarMensajePorIndice(this.usuario, indice, tipo);
            escritor.println(eliminado ? "Mensaje eliminado." : "No se pudo eliminar.");
        } catch (NumberFormatException e) {
            escritor.println("Número inválido.");
        }
    }

    private void leerMensajesPaginados() throws IOException {
        List<String> mensajes = GestorDeDatos.obtenerMensajesRecibidos(this.usuario);
        if (mensajes.isEmpty()) {
            escritor.println("NO_HAY_MENSAJES");
            return;
        }
        escritor.println("HAY_MENSAJES");
        int porPagina = 3;
        for (int i = 0; i < mensajes.size(); i++) {
            escritor.println(mensajes.get(i));
            boolean esUltimoDePagina = (i + 1) % porPagina == 0;
            boolean esUltimoMensaje = i == mensajes.size() - 1;
            if ((esUltimoDePagina || esUltimoMensaje) && !esUltimoMensaje) {
                escritor.println("--- Página " + ((i / porPagina) + 1) + " ---");
                escritor.println("MAS_PAGINAS");
                escritor.println("¿Quieres continuar? (escribe 'siguiente')");
                if (!"siguiente".equalsIgnoreCase(lector.readLine())) break;
            }
        }
        escritor.println("FIN_LISTA");
    }

    private void eliminarCuenta() throws IOException {
        if ("si".equalsIgnoreCase(lector.readLine())) {
            boolean eliminado = GestorDeDatos.eliminarUsuarioCompleto(this.usuario);
            escritor.println(eliminado ? "ELIMINADO_OK" : "ELIMINADO_ERROR");
        } else {
            escritor.println("ELIMINADO_CANCELADO");
        }
    }

    private void bloquearUsuario() throws IOException {
        String usuarioABloquear = lector.readLine();
        boolean bloqueado = GestorDeDatos.bloquearUsuario(this.usuario, usuarioABloquear);
        escritor.println(bloqueado ? "Usuario bloqueado." : "Error al bloquear.");
    }

    private void desbloquearUsuario() throws IOException {
        String usuarioADesbloquear = lector.readLine();
        boolean desbloqueado = GestorDeDatos.desbloquearUsuario(this.usuario, usuarioADesbloquear);
        escritor.println(desbloqueado ? "Usuario desbloqueado." : "Error al desbloquear.");
    }

    private void solicitarVerArchivos() throws IOException {
        String usuarioObjetivo = lector.readLine();
        if (GestorDeDatos.existeUsuario(usuarioObjetivo)) {
            solicitudesArchivos.put(usuarioObjetivo, this.usuario);
            escritor.println("✅ Solicitud enviada a " + usuarioObjetivo + ".");
        } else {
            escritor.println("❌ Usuario no registrado.");
        }
    }

    private void gestionarSolicitudDeArchivos() throws IOException {
        if (solicitudesArchivos.containsKey(this.usuario)) {
            String solicitante = solicitudesArchivos.get(this.usuario);
            escritor.println("SOLICITUD_ARCHIVOS");
            escritor.println(solicitante);

            if ("si".equalsIgnoreCase(lector.readLine())) {
                escritor.println("PEDIR_LISTA_Y_CONTENIDO");

                Map<String, List<String>> archivosRecibidos = new HashMap<>();
                String linea;
                while (!(linea = lector.readLine()).equals("_END_ALL_FILES_")) {
                    String nombreArchivo = linea;
                    List<String> contenido = new ArrayList<>();
                    while (!(linea = lector.readLine()).equals("_ENDFILE_")) {
                        contenido.add(linea);
                    }
                    archivosRecibidos.put(nombreArchivo, contenido);
                }
                archivosCompartidos.put(solicitante, archivosRecibidos);
                escritor.println("✅ Archivos y contenido enviados al servidor.");
            } else {
                escritor.println("❌ Solicitud rechazada.");
            }
            solicitudesArchivos.remove(this.usuario);
        } else {
            escritor.println("No tienes solicitudes pendientes.");
        }
        escritor.println("FIN_SOLICITUDES");
    }

    private void verYCopiarArchivosCompartidos() throws IOException {
        if (archivosCompartidos.containsKey(this.usuario)) {
            Map<String, List<String>> misArchivos = archivosCompartidos.get(this.usuario);
            escritor.println("--- Archivos que te compartieron ---");
            misArchivos.keySet().forEach(escritor::println);
            escritor.println("FIN_LISTA_ARCHIVOS");

            String decisionCopia = lector.readLine();
            if ("_REQUEST_COPY_".equals(decisionCopia)) {
                String archivoSolicitado = lector.readLine();
                if (misArchivos.containsKey(archivoSolicitado)) {
                    misArchivos.get(archivoSolicitado).forEach(escritor::println);
                } else {
                    escritor.println("Error: El archivo no existe.");
                }
                escritor.println("_ENDFILE_");
            }
            archivosCompartidos.remove(this.usuario);
        } else {
            escritor.println("No tienes archivos compartidos para ver.");
        }
    }
}

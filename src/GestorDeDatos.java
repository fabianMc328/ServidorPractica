

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GestorDeDatos {
    private static final String ARCHIVO_USUARIOS = "archivo.txt";
    private static final String ARCHIVO_MENSAJES = "mensajes.txt";
    private static final String ARCHIVO_BLOQUEOS = "bloqueados.txt";

    public static synchronized boolean registrarUsuario(String usuario, String contrasena) {
        if (existeUsuario(usuario)) return false;
        try (PrintWriter pw = new PrintWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            pw.println(usuario + ";" + contrasena);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean validarCredenciales(String usuario, String contrasena) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";", 2);
                if (datos.length == 2 && datos[0].equals(usuario) && datos[1].equals(contrasena)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static synchronized boolean existeUsuario(String usuario) {
        return leerLineasDeArchivo(ARCHIVO_USUARIOS).stream()
                .anyMatch(linea -> linea.startsWith(usuario + ";"));
    }

    public static synchronized List<String> obtenerListaDeUsuarios() {
        return leerLineasDeArchivo(ARCHIVO_USUARIOS).stream()
                .map(linea -> linea.split(";", 2)[0])
                .collect(Collectors.toList());
    }

    public static synchronized boolean eliminarUsuarioCompleto(String usuario) {
        List<String> usuarios = leerLineasDeArchivo(ARCHIVO_USUARIOS);
        boolean eliminado = usuarios.removeIf(linea -> linea.startsWith(usuario + ";"));
        escribirLineasEnArchivo(ARCHIVO_USUARIOS, usuarios, false);

        List<String> mensajes = leerLineasDeArchivo(ARCHIVO_MENSAJES);
        mensajes.removeIf(linea -> {
            String[] datos = linea.split(";", 3);
            return datos[0].equals(usuario) || datos[1].equals(usuario);
        });
        escribirLineasEnArchivo(ARCHIVO_MENSAJES, mensajes, false);

        List<String> bloqueos = leerLineasDeArchivo(ARCHIVO_BLOQUEOS);
        bloqueos.removeIf(linea -> {
            String[] datos = linea.split(";", 2);
            return datos[0].equals(usuario) || datos[1].equals(usuario);
        });
        escribirLineasEnArchivo(ARCHIVO_BLOQUEOS, bloqueos, false);

        return eliminado;
    }

    public static synchronized void guardarMensaje(String remitente, String dest, String msg) {
        escribirLineasEnArchivo(ARCHIVO_MENSAJES,
                Collections.singletonList(remitente + ";" + dest + ";" + msg), true);
    }

    public static synchronized List<String> obtenerMensajesRecibidos(String usuario) {
        return leerLineasDeArchivo(ARCHIVO_MENSAJES).stream()
                .filter(linea -> linea.split(";", 3)[1].equals(usuario))
                .map(linea -> {
                    String[] datos = linea.split(";", 3);
                    return "De " + datos[0] + ": " + datos[2];
                })
                .collect(Collectors.toList());
    }

    public static synchronized List<String> obtenerMensajesPorTipo(String usuario, String tipo) {
        List<String> mensajesFiltrados = new ArrayList<>();
        int contador = 1;
        for (String linea : leerLineasDeArchivo(ARCHIVO_MENSAJES)) {
            String[] datos = linea.split(";", 3);
            if ("recibido".equalsIgnoreCase(tipo) && datos[1].equals(usuario)) {
                mensajesFiltrados.add(contador++ + ". De " + datos[0] + ": " + datos[2]);
            } else if ("enviado".equalsIgnoreCase(tipo) && datos[0].equals(usuario)) {
                mensajesFiltrados.add(contador++ + ". Para " + datos[1] + ": " + datos[2]);
            }
        }
        return mensajesFiltrados;
    }

    public static synchronized boolean eliminarMensajePorIndice(String usuario, int indice, String tipo) {
        List<String> todosLosMensajes = leerLineasDeArchivo(ARCHIVO_MENSAJES);
        List<String> mensajesDelUsuario = new ArrayList<>();
        List<String> otrosMensajes = new ArrayList<>();

        for (String linea : todosLosMensajes) {
            String[] datos = linea.split(";", 3);
            boolean esRecibido = "recibido".equalsIgnoreCase(tipo) && datos[1].equals(usuario);
            boolean esEnviado = "enviado".equalsIgnoreCase(tipo) && datos[0].equals(usuario);
            if (esRecibido || esEnviado) {
                mensajesDelUsuario.add(linea);
            } else {
                otrosMensajes.add(linea);
            }
        }

        if (indice < 1 || indice > mensajesDelUsuario.size()) return false;

        mensajesDelUsuario.remove(indice - 1);
        otrosMensajes.addAll(mensajesDelUsuario);
        escribirLineasEnArchivo(ARCHIVO_MENSAJES, otrosMensajes, false);
        return true;
    }

    public static synchronized boolean bloquearUsuario(String bloqueador, String aBloquear) {
        if (!existeUsuario(aBloquear)) return false;
        escribirLineasEnArchivo(ARCHIVO_BLOQUEOS,
                Collections.singletonList(bloqueador + ";" + aBloquear), true);
        return true;
    }

    public static synchronized boolean desbloquearUsuario(String desbloqueador, String aDesbloquear) {
        List<String> bloqueos = leerLineasDeArchivo(ARCHIVO_BLOQUEOS);
        boolean eliminado = bloqueos.removeIf(linea -> linea.equals(desbloqueador + ";" + aDesbloquear));
        escribirLineasEnArchivo(ARCHIVO_BLOQUEOS, bloqueos, false);
        return eliminado;
    }

    public static synchronized boolean estaBloqueado(String usuario, String remitente) {
        return leerLineasDeArchivo(ARCHIVO_BLOQUEOS).stream()
                .anyMatch(linea -> linea.equals(usuario + ";" + remitente));
    }

    private static List<String> leerLineasDeArchivo(String nombreArchivo) {
        List<String> lineas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(nombreArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas.add(linea);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineas;
    }

    private static void escribirLineasEnArchivo(String nombreArchivo, List<String> lineas, boolean agregar) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(nombreArchivo, agregar))) {
            for (String linea : lineas) {
                pw.println(linea);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

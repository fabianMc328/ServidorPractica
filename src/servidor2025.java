import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.spec.RSAOtherPrimeInfo;
import java.text.CollationElementIterator;

public class servidor2025 {

    private static final String ARCHIVO_USUARIOS = "archivo.txt";

    public static  void main(String[] args) throws IOException {
        ServerSocket socketespecial = null;

        String rutaArchivo = "C:\\Users\\M4-MQ12-D\\Desktop\\archivito\\archivo.txt";

        try {

            FileWriter escritor = new FileWriter(rutaArchivo);

            escritor.close();


        } catch (IOException e) {
            System.out.println("Error al crear el archivo.");
            e.printStackTrace();
        }

        try {
            socketespecial = new ServerSocket(8080);
        } catch (IOException e) {
            System.out.println("hubo problemas en la red");
            System.exit(1);

        }
        Socket cliente = null;
        try {
            cliente = socketespecial.accept();
        }catch (IOException e){

            System.out.println("hubo problemas en la red 2.0");
            System.exit(1);
        }

        PrintWriter escritor =null;
        try {
            escritor = new PrintWriter(cliente.getOutputStream(), true);

            BufferedReader lectorSocket = new BufferedReader(new InputStreamReader(//agarro los datos que me manda el cliente
                    cliente.getInputStream()));
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
            String entrada;
            String mensaje;
            String opcion = lectorSocket.readLine();

            if ("1".equals(opcion)) {
                // Registrar usuario
                String usuario = lectorSocket.readLine();
                String contrasena = lectorSocket.readLine();
                if (registrarUsuario(usuario, contrasena)) {
                    escritor.println("✅ Usuario registrado con éxito.");
                } else {
                    escritor.println("❌ El usuario ya existe.");
                }
            } else if ("2".equals(opcion)) {
                // Iniciar sesión
                String usuario = lectorSocket.readLine();
                String contrasena = lectorSocket.readLine();
                if (validarLogin(usuario, contrasena)) {
                    escritor.println("✅ Bienvenido, " + usuario + "!");
                } else {
                    escritor.println("❌ Usuario o contraseña incorrectos.");
                }
            } else {
                escritor.println("❌ Opción no válida.");
            }

            socketespecial.close();
/*
-
            int numero = (in--t)(Math.random() * 10) + 1; // entre 1 y 10
            int intentos = 0;
            escritor.println("Adivina el número del 1 al 10");
            while ((lectorSocket = lectorSocket.readLine()) != null) {
                try {
                    int evaluar = Integer.parseInt(lectorSocket);

                    if (intentos < 2 ) {



                        if (evaluar > numero) {
                            escritor.println("es menor");

                            intentos++;
                        }
                        if (evaluar < numero) {
                            escritor.println("es mayor");
                            intentos++;
                        }
                        if (evaluar == numero) {
                            escritor.println("ese es el numero felicidades");
                            break;
                        }




                    }else{
                        escritor.println("no pudiste adivinar el numero");
                        cliente.close();
                        socketespecial.close();

                    }
                }catch (NumberFormatException e) { escritor.println("debe ser un numero");
                }

 */
            /*
                // System.out.println(lectorSocket.toUpperCase());//imprimo los datos
                //  System.out.println(numero);
                //      mensaje = teclado.readLine();
                //     escritor.println(mensaje);
           /*   if (mensaje.equalsIgnoreCase("fin")) {
                    break;
                }
*/


            cliente.close();
            socketespecial.close();
        } catch (Exception e) {
            System.out.println("hubo problemas en la conexion de los sockets");
            System.exit(2);
        }
        try {
            cliente.close();
        } catch (IOException e) {
            System.out.println("hubo problemas en la conexion dE RED");
            System.exit(1);
        }



    }
    public static boolean registrarUsuario(String usuario, String contrasena) {
        // Revisar si el usuario ya existe
        if (validarExistencia(usuario)) {
            return false;
        }

        try (FileWriter fw = new FileWriter(ARCHIVO_USUARIOS, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {

            pw.println(usuario + ";" + contrasena);
            return true;
        } catch (IOException e) {
            System.out.println("Error al guardar usuario.");
            return false;
        }
    }

    public static  boolean validarLogin(String usuario, String contrasena) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2 && datos[0].equals(usuario) && datos[1].equals(contrasena)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Error al leer el archivo.");
        }
        return false;
    }
    public static  boolean validarExistencia(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 2 && datos[0].equals(usuario)) {
                    return true; // ya existe
                }
            }
        } catch (IOException e) {
            // Si el archivo no existe aún, no hay usuarios
        }
        return false;
    }
}


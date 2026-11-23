package restaurante;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Usuarios del panel, con clave protegida (SHA-256) y permisos según el rol. */
public final class Usuarios {
    private Usuarios() {}

    public enum Rol {
        ADMIN("Administrador"), CAJERO("Cajero"), MESERO("Mesero");

        private final String etiqueta;
        Rol(String etiqueta) { this.etiqueta = etiqueta; }
        public String etiqueta() { return etiqueta; }
        public boolean esAdmin() { return this == ADMIN; }
        public boolean puedeCobrar() { return this != MESERO; }
        public boolean puedeVerVentas() { return this != MESERO; }
        public boolean puedeVerMenu() { return this != MESERO; }
        public boolean puedeEditarMenu() { return this == ADMIN; }
        @Override public String toString() { return etiqueta; }
    }

    public record Usuario(String nombre, Rol rol) { }

    private record Registro(String nombre, String hash, Rol rol) { }

    private static final Path ARCHIVO = Path.of("usuarios.csv");
    private static final String CLAVE_INICIAL = "admin123";
    private static final List<Registro> LISTA = new ArrayList<>();

    static {
        cargar();
        if (LISTA.isEmpty()) {
            LISTA.add(new Registro("admin", hash("admin", CLAVE_INICIAL), Rol.ADMIN));
            guardar();
        }
    }

    private static String hash(String nombre, String clave) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest((nombre.toLowerCase() + ":" + clave).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Registro buscar(String nombre) {
        for (Registro r : LISTA) if (r.nombre().equalsIgnoreCase(nombre)) return r;
        return null;
    }

    private static long admins() { return LISTA.stream().filter(r -> r.rol() == Rol.ADMIN).count(); }

    /** True mientras el usuario "admin" siga con la clave de fábrica. */
    public static synchronized boolean claveInicial() {
        Registro r = buscar("admin");
        return r != null && r.hash().equals(hash("admin", CLAVE_INICIAL));
    }

    public static synchronized Usuario entrar(String nombre, String clave) {
        Registro r = buscar(nombre.trim());
        if (r == null || !r.hash().equals(hash(r.nombre(), clave))) return null;
        return new Usuario(r.nombre(), r.rol());
    }

    public static synchronized List<Usuario> lista() {
        List<Usuario> l = new ArrayList<>();
        for (Registro r : LISTA) l.add(new Usuario(r.nombre(), r.rol()));
        return l;
    }

    /** Cada método devuelve un mensaje de error, o null si todo salió bien. */
    public static synchronized String crear(String nombre, String clave, Rol rol) {
        nombre = nombre.trim();
        if (nombre.length() < 3 || nombre.contains(";") || nombre.contains(" ")) return "El usuario necesita 3 letras o más, sin espacios ni ';'.";
        if (clave.length() < 4) return "La clave debe tener al menos 4 caracteres.";
        if (buscar(nombre) != null) return "Ese usuario ya existe.";
        LISTA.add(new Registro(nombre, hash(nombre, clave), rol));
        guardar();
        return null;
    }

    public static synchronized String cambiarClave(String nombre, String nueva) {
        Registro r = buscar(nombre);
        if (r == null) return "Usuario no encontrado.";
        if (nueva.length() < 4) return "La clave debe tener al menos 4 caracteres.";
        LISTA.set(LISTA.indexOf(r), new Registro(r.nombre(), hash(r.nombre(), nueva), r.rol()));
        guardar();
        return null;
    }

    public static synchronized String cambiarRol(String nombre, Rol rol) {
        Registro r = buscar(nombre);
        if (r == null) return "Usuario no encontrado.";
        if (r.rol() == Rol.ADMIN && rol != Rol.ADMIN && admins() <= 1) return "Debe quedar al menos un administrador.";
        LISTA.set(LISTA.indexOf(r), new Registro(r.nombre(), r.hash(), rol));
        guardar();
        return null;
    }

    public static synchronized String eliminar(String nombre, String actual) {
        Registro r = buscar(nombre);
        if (r == null) return "Usuario no encontrado.";
        if (r.nombre().equalsIgnoreCase(actual)) return "No puedes eliminar tu propio usuario.";
        if (r.rol() == Rol.ADMIN && admins() <= 1) return "Debe quedar al menos un administrador.";
        LISTA.remove(r);
        guardar();
        return null;
    }

    private static void cargar() {
        try {
            for (String linea : Files.readAllLines(ARCHIVO, StandardCharsets.UTF_8)) {
                String[] p = linea.split(";");
                if (p.length == 3) LISTA.add(new Registro(p[0], p[1], Rol.valueOf(p[2])));
            }
        } catch (IOException | IllegalArgumentException e) {
            // sin archivo todavía
        }
    }

    private static void guardar() {
        List<String> l = new ArrayList<>();
        for (Registro r : LISTA) l.add(r.nombre() + ";" + r.hash() + ";" + r.rol().name());
        try {
            Files.write(ARCHIVO, l, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudieron guardar los usuarios: " + e.getMessage());
        }
    }
}

import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

public class DetectarErroresCodificacion {

    public static void main(String[] args) throws IOException {
        Path rootPath = args.length > 0 ? Paths.get(args[0]) : Paths.get(".");
        List<String> resultados = new ArrayList<>();
        resultados.add("Archivo,Codificacion,LineasConError");

        Files.walk(rootPath)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> {
                    try {
                        String codificacion = detectarCodificacion(file);
                        if (codificacion.equals("Desconocida")) {
                            codificacion = "UTF-8"; // fallback
                        }
                        Charset charset = Charset.forName(codificacion);

                        List<Integer> lineasConError = new ArrayList<>();

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile()), charset))) {
                            String linea;
                            int numLinea = 0;
                            while ((linea = reader.readLine()) != null) {
                                numLinea++;
                                // Detectar caracteres problemáticos: \uFFFD (carácter de reemplazo) y ï¿½
                                if (linea.contains("\uFFFD") || linea.contains("ï¿½")) {
                                    lineasConError.add(numLinea);
                                }
                            }
                        }

                        if (!lineasConError.isEmpty()) {
                            System.out.printf("[ERROR] %s | Codificación: %s | Líneas: %s%n",
                                    file, codificacion, lineasConError);
                            resultados.add(String.format("\"%s\",\"%s\",\"%s\"",
                                    file.toAbsolutePath(),
                                    codificacion,
                                    lineasConError.toString().replaceAll("[\\[\\]]", "")));
                        }

                    } catch (Exception e) {
                        System.err.println("Error procesando archivo: " + file + " -> " + e.getMessage());
                    }
                });

        // Guardar CSV
        Path salida = rootPath.resolve("errores_codificacion.csv");
        Files.write(salida, resultados, Charset.forName("UTF-8"));
        System.out.println("\nReporte guardado en: " + salida.toAbsolutePath());
    }

    private static String detectarCodificacion(Path file) throws IOException {
        try (InputStream is = new FileInputStream(file.toFile())) {
            byte[] buf = new byte[4096];
            UniversalDetector detector = new UniversalDetector(null);

            int n;
            while ((n = is.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, n);
            }
            detector.dataEnd();

            String encoding = detector.getDetectedCharset();
            detector.reset();

            return encoding != null ? encoding : "Desconocida";
        }
    }
}

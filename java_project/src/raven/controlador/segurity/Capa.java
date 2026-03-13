package raven.controlador.segurity;

public class Capa {
    public static String transformacionModularInversa(String mensajeTransformado) {
        String[] palabras = mensajeTransformado.split(" ");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            String nuevaPalabra = palabra.substring(2) + palabra.substring(0, 2);
            resultado.append(nuevaPalabra).append(" ");
        }
        return resultado.toString().trim();
    }
    public static String desplazamientoCircularInverso(String mensajeInvertido) {
        String[] palabras = mensajeInvertido.split(" ");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            StringBuilder nuevaPalabra = new StringBuilder();
            for (int i = 0; i < palabra.length(); i++) {
                char letra = palabra.charAt(i);
                if (Character.isLetter(letra)) {
                    if (i % 2 == 0) {
                        if (Character.isLowerCase(letra)) {
                            letra = (char) ((letra - 'a' - 5 + 26) % 26 + 'a');
                        } else {
                            letra = (char) ((letra - 'A' - 5 + 26) % 26 + 'A');
                        }
                    } else {
                        if (Character.isLowerCase(letra)) {
                            letra = (char) ((letra - 'a' + 7) % 26 + 'a');
                        } else {
                            letra = (char) ((letra - 'A' + 7) % 26 + 'A');
                        }
                    }
                }

                nuevaPalabra.append(letra);
            }
            resultado.append(nuevaPalabra).append(" ");
        }

        return resultado.toString().trim();
    }


    public static String invertirPalabraInversa(String mensaje) {
        String[] palabras = mensaje.split(" ");
        StringBuilder resultado = new StringBuilder();

        for (String palabra : palabras) {
            String palabraInvertida = new StringBuilder(palabra).reverse().toString();
            resultado.append(palabraInvertida).append(" ");
        }

        return resultado.toString().trim();
    }


    public static String Luisa(String mensaje) {
        String paso1 = transformacionModularInversa(mensaje);
        String paso2 = desplazamientoCircularInverso(paso1);
        String paso3 = invertirPalabraInversa(paso2);
        return paso3;
    }
   public static final String host = "51222.052.21.4";
   public static final String datsbase = "hxjfjkyq_mk";
   public static final String user = "nlsbrwf_tk";
   public static final String password = "bXfujnwI$5202hw";
}


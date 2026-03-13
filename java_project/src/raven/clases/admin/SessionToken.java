package raven.clases.admin;

import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generador de tokens de sesión seguros.
 * 
 * Aplica principios SOLID:
 * - Single Responsibility: Solo genera y valida tokens
 * - Open/Closed: Extensible mediante interfaces
 * 
 * @author CrisDEV
 */
public class SessionToken {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // 256 bits
    
    /**
     * Genera un token de sesión único y seguro.
     * 
     * Formato: BASE64(RANDOM_BYTES + TIMESTAMP_HASH)
     * 
     * @param userId ID del usuario
     * @return Token de sesión único
     */
    public static String generate(int userId) {
        try {
            // 1. Generar bytes aleatorios
            byte[] randomBytes = new byte[TOKEN_LENGTH];
            SECURE_RANDOM.nextBytes(randomBytes);
            
            // 2. Agregar timestamp + userId para unicidad
            String uniqueData = userId + "_" + System.currentTimeMillis();
            
            // 3. Hash SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(uniqueData.getBytes());
            
            // 4. Combinar random + hash
            byte[] combined = new byte[randomBytes.length + hash.length];
            System.arraycopy(randomBytes, 0, combined, 0, randomBytes.length);
            System.arraycopy(hash, 0, combined, randomBytes.length, hash.length);
            
            // 5. Codificar en Base64 (seguro para URLs)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback a UUID si SHA-256 no está disponible
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }
    
    /**
     * Valida el formato de un token.
     * 
     * @param token Token a validar
     * @return true si el formato es válido
     */
    public static boolean isValidFormat(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Base64 URL-safe sin padding tiene longitud específica
        int expectedLength = (TOKEN_LENGTH + 32) * 4 / 3; // Aproximado
        return token.length() >= expectedLength - 5 && token.length() <= expectedLength + 5;
    }
}
package uy.edu.utu.shopexpress.usuarios.privacidad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Técnicas de privacidad aplicadas a datos personales:
 *  - Cifrado AES-256-GCM (cifrado autenticado) para guardar el documento en reposo.
 *  - Seudonimización con HMAC-SHA256: misma entrada produce mismo seudónimo, pero sin
 *    la clave no se puede revertir ni recalcular por fuerza bruta.
 *  - Enmascarado para mostrar datos parciales.
 */
@Component
public class ProteccionDatos {

    private static final String ALGORITMO_CIFRADO = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey claveAes;
    private final SecretKeySpec claveHmac;
    private final SecureRandom random = new SecureRandom();

    public ProteccionDatos(@Value("${privacidad.clave-aes}") String claveAesBase64,
                           @Value("${privacidad.clave-hmac}") String claveHmac) {
        byte[] bytes = Base64.getDecoder().decode(claveAesBase64);
        if (bytes.length != 32) {
            throw new IllegalStateException("privacidad.clave-aes debe tener 32 bytes (AES-256) codificados en Base64");
        }
        this.claveAes = new SecretKeySpec(bytes, "AES");
        this.claveHmac = new SecretKeySpec(claveHmac.getBytes(UTF_8), "HmacSHA256");
    }

    /** Devuelve Base64(IV || texto cifrado || tag). Cada llamada usa un IV aleatorio. */
    public String cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRADO);
            cipher.init(Cipher.ENCRYPT_MODE, claveAes, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    public String descifrar(String base64) {
        try {
            byte[] datos = Base64.getDecoder().decode(base64);
            byte[] iv = Arrays.copyOfRange(datos, 0, IV_BYTES);
            byte[] cifrado = Arrays.copyOfRange(datos, IV_BYTES, datos.length);
            Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRADO);
            cipher.init(Cipher.DECRYPT_MODE, claveAes, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar el dato", e);
        }
    }

    /** Seudónimo HMAC-SHA256 en hexadecimal (64 caracteres). */
    public String seudonimizar(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(claveHmac);
            return HexFormat.of().formatHex(mac.doFinal(valor.getBytes(UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo seudonimizar el dato", e);
        }
    }

    /** juan.perez@example.com -> j*********@example.com */
    public static String enmascararEmail(String email) {
        if (email == null) {
            return null;
        }
        int arroba = email.indexOf('@');
        if (arroba <= 1) {
            return "***" + (arroba >= 0 ? email.substring(arroba) : "");
        }
        return email.charAt(0) + "*".repeat(arroba - 1) + email.substring(arroba);
    }

    /** 12345678 -> *****678 */
    public static String enmascararDocumento(String documento) {
        if (documento == null || documento.length() <= 3) {
            return "***";
        }
        int visibles = 3;
        return "*".repeat(documento.length() - visibles) + documento.substring(documento.length() - visibles);
    }
}

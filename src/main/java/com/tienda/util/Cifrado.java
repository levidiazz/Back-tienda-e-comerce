package com.tienda.util;

import com.tienda.conexion.Configuracion;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Cifrado {

    private static final String PREFIJO = "v1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final SecretKeySpec CLAVE = cargarClave();

    private Cifrado() {
    }

    private static SecretKeySpec cargarClave() {
        String clave = Configuracion.obtener("cifrado.clave");
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Falta cifrado.clave en db.properties");
        }
        byte[] bytes = Base64.getDecoder().decode(clave.trim());
        if (bytes.length != 32) {
            throw new IllegalStateException("cifrado.clave debe ser de 32 bytes codificados en Base64");
        }
        return new SecretKeySpec(bytes, "AES");
    }

    public static String cifrar(String texto) {
        if (texto == null) {
            return texto;
        }
        try {
            byte[] iv = new byte[BYTES_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, CLAVE, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] resultado = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIJO + Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato", e);
        }
    }

    public static String descifrar(String valor) {
        if (valor == null || !estaCifrado(valor)) {
            return valor;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, CLAVE, new GCMParameterSpec(BITS_ETIQUETA, datos, 0, BYTES_IV));
            byte[] texto = cipher.doFinal(datos, BYTES_IV, datos.length - BYTES_IV);
            return new String(texto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar un dato; revisa cifrado.clave", e);
        }
    }

    public static boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    public static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

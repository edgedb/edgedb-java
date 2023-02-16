package com.edgedb.driver.util;

import com.edgedb.driver.binary.packets.receivable.AuthenticationStatus;
import com.edgedb.driver.binary.packets.sendables.AuthenticationSASLInitialResponse;
import com.edgedb.driver.binary.packets.sendables.AuthenticationSASLResponse;
import com.edgedb.driver.exceptions.ScramException;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Scram {
    private static final int NONCE_LENGTH = 18;
    private static final SecureRandom random = new SecureRandom();
    private static final Pattern serverMessageParser = Pattern.compile("(.)=(.+?)(?>,|$)");

    private String rawFirstMessage;
    private byte[] clientNonce;

    public Scram() {}

    public Scram(byte[] clientNonce) {
        this.clientNonce = clientNonce;
    }

    private static byte[] generateNonce() {
        var bytes = new byte[NONCE_LENGTH];

        random.nextBytes(bytes);

        return bytes;
    }

    private static ByteBuffer encodeString(String s) {
        var buffer = ByteBuffer.allocateDirect(BinaryProtocolUtils.sizeOf(s));
        var encoded = s.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(encoded.length);
        buffer.put(encoded);
        return buffer;
    }

    private static String decodeString(ByteBuffer buffer) {
        var len = buffer.getInt();
        byte[] strBytes = new byte[len];

        buffer.get(strBytes);

        return new String(strBytes, StandardCharsets.UTF_8);
    }

    public String buildInitialMessage(String username) {
        if(this.clientNonce == null) {
            this.clientNonce = generateNonce();
        }

        this.rawFirstMessage = String.format("n=%s,r=%s", Normalizer.normalize(username, Normalizer.Form.NFKC), Base64.getEncoder().encodeToString(this.clientNonce));
        return "n,," + this.rawFirstMessage;
    }

    public AuthenticationSASLInitialResponse buildInitialMessagePacket(String username, String method) {
        var initial = buildInitialMessage(username);
        return new AuthenticationSASLInitialResponse(encodeString(initial), method);
    }

    public static class SASLFinalMessage {
        public final String message;
        public final byte[] signature;

        public SASLFinalMessage(String message, byte[] signature) {
            this.message = message;
            this.signature = signature;
        }

        public AuthenticationSASLResponse buildPacket() {
            return new AuthenticationSASLResponse(encodeString(this.message));
        }
    }

    public SASLFinalMessage buildFinalMessage(AuthenticationStatus status, String password) throws ScramException {
        assert status.saslData != null;
        return buildFinalMessage(decodeString(status.saslData), password);
    }

    public SASLFinalMessage buildFinalMessage(String initialResponse, String password) throws ScramException {
        var parsed = parseServerMessage(initialResponse);

        if(parsed.size() < 3) {
            throw new ScramException();
        }

        var salt = Base64.getDecoder().decode(parsed.get("s"));

        int iterations;

        try {
            iterations = Integer.parseInt(parsed.get("i"));
        }
        catch (NumberFormatException e) {
            throw new ScramException(e);
        }

        var finalMessage = "c=biws,r=" + parsed.get("r");
        var authMessage = String.format("%s,%s,%s", this.rawFirstMessage, initialResponse, finalMessage).getBytes(StandardCharsets.UTF_8);

        var saltedPassword = saltPassword(password, salt, iterations);
        var clientKey = getClientKey(saltedPassword);
        var storedKey = hash(clientKey);
        var clientSig = computeHMACHash(storedKey, authMessage);
        var clientProof = xor(clientKey, clientSig);

        var serverKey = getServerKey(saltedPassword);
        var serverProof = computeHMACHash(serverKey, authMessage);

        return new SASLFinalMessage(
                String.format("%s,p=%s", finalMessage, Base64.getEncoder().encodeToString(clientProof)),
                serverProof
        );
    }

    public static byte[] parseServerFinalMessage(AuthenticationStatus status) {
        assert status.saslData != null;
        var message = decodeString(status.saslData);

        var parsed = parseServerMessage(message);

        return Base64.getDecoder().decode(parsed.get("v"));
    }

    private static byte[] saltPassword(String password, byte[] salt, int iterations) throws ScramException {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations);

        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();

        } catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
            throw new ScramException(e);
        }
    }

    private static byte[] getClientKey(byte[] password) throws ScramException {
        return computeHMACHash(password, "Client Key");
    }

    private static byte[] getServerKey(byte[] password) throws ScramException {
        return computeHMACHash(password, "Server Key");
    }

    private static byte[] computeHMACHash(byte[] data, String key) throws ScramException {
        return computeHMACHash(data, key.getBytes(StandardCharsets.UTF_8));
    }
    private static byte[] computeHMACHash(byte[] data, byte[] key) throws ScramException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "SHA256");

        try {
            var mac = Mac.getInstance("SHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException|InvalidKeyException e) {
            throw new ScramException(e);
        }

    }

    private static byte[] hash(byte[] data) throws ScramException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new ScramException(e);
        }
    }

    private static byte[] xor(byte[] b1, byte[] b2) {
        var length = b1.length;

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
        {
            result[i] = (byte)(b1[i] ^ b2[i]);
        }

        return result;
    }

    private static Map<String, String> parseServerMessage(String message) {
        var matcher = serverMessageParser.matcher(message);
        return matcher.results().collect(Collectors.toMap((v) -> v.group(1), (v) -> v.group(2)));
    }
}

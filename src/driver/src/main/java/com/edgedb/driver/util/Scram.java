package com.edgedb.driver.util;

import com.edgedb.driver.binary.packets.receivable.AuthenticationStatus;
import com.edgedb.driver.binary.packets.sendables.AuthenticationSASLInitialResponse;
import com.edgedb.driver.binary.packets.sendables.AuthenticationSASLResponse;
import com.edgedb.driver.exceptions.ScramException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
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

    private static @NotNull ByteBuf encodeString(@NotNull String s) {
        var buffer = ByteBufAllocator.DEFAULT.buffer(BinaryProtocolUtils.sizeOf(s) - 4); // no str length
        var encoded = s.getBytes(StandardCharsets.UTF_8);
        buffer.writeBytes(encoded);
        return buffer;
    }

    private static @NotNull String decodeString(@NotNull ByteBuf buffer) {
        byte[] strBytes = new byte[buffer.readableBytes()];

        buffer.readBytes(strBytes);

        return new String(strBytes, StandardCharsets.UTF_8);
    }

    public @NotNull String buildInitialMessage(@NotNull String username) {
        if(this.clientNonce == null) {
            this.clientNonce = generateNonce();
        }

        this.rawFirstMessage = String.format("n=%s,r=%s", Normalizer.normalize(username, Normalizer.Form.NFKC), Base64.getEncoder().encodeToString(this.clientNonce));
        return "n,," + this.rawFirstMessage;
    }

    public @NotNull AuthenticationSASLInitialResponse buildInitialMessagePacket(@NotNull String username, String method) {
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

        public @NotNull AuthenticationSASLResponse buildPacket() {
            return new AuthenticationSASLResponse(encodeString(this.message));
        }
    }

    public @NotNull SASLFinalMessage buildFinalMessage(@NotNull AuthenticationStatus status, @NotNull String password) throws ScramException {
        assert status.saslData != null;
        return buildFinalMessage(decodeString(status.saslData), password);
    }

    public @NotNull SASLFinalMessage buildFinalMessage(@NotNull String initialResponse, @NotNull String password) throws ScramException {
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

    public static byte[] parseServerFinalMessage(@NotNull AuthenticationStatus status) {
        assert status.saslData != null;
        var message = decodeString(status.saslData);

        var parsed = parseServerMessage(message);

        return Base64.getDecoder().decode(parsed.get("v"));
    }

    private static byte[] saltPassword(@NotNull String password, byte @NotNull [] salt, int iterations) throws ScramException {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);

        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();

        } catch (NoSuchAlgorithmException|InvalidKeySpecException e) {
            throw new ScramException(e);
        }
    }

    private static byte[] getClientKey(byte @NotNull [] password) throws ScramException {
        return computeHMACHash(password, "Client Key");
    }

    private static byte[] getServerKey(byte @NotNull [] password) throws ScramException {
        return computeHMACHash(password, "Server Key");
    }

    private static byte[] computeHMACHash(byte @NotNull [] data, @NotNull String key) throws ScramException {
        return computeHMACHash(data, key.getBytes(StandardCharsets.UTF_8));
    }
    private static byte[] computeHMACHash(byte @NotNull [] data, byte[] key) throws ScramException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(data, "SHA256");

        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(key);
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

    private static byte[] xor(byte @NotNull [] b1, byte[] b2) {
        var length = b1.length;

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
        {
            result[i] = (byte)(b1[i] ^ b2[i]);
        }

        return result;
    }

    private static @NotNull Map<String, String> parseServerMessage(@NotNull String message) {
        var matcher = serverMessageParser.matcher(message);
        return matcher.results().collect(Collectors.toMap((v) -> v.group(1), (v) -> v.group(2)));
    }
}

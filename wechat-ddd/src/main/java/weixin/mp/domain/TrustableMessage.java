package weixin.mp.domain;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 消息加密避免消息外泄，消息摘要避免篡改
 *
 * <a href="https://www.oracle.com/java/technologies/javase-jce-all-downloads.html">JDK9起无加解密密钥长度等限制</a>
 */
public interface TrustableMessage {

    String digest(String content);

    MessageWrapper decrypt(String key, String ciphertext);

    String encrypt(String key, byte[] plaintext);

    TrustableMessage MP_INSTANCE = new TrustableMessage() {

        private static final String DIGEST_ALGORITHM = "SHA-1";

        private static final String CIPHER_ALGORITHM = "AES/CBC/NoPadding";

        private static final String KEY_ALGORITHM = "AES";

        private static final int OFFSET = 16;

        private static final int CONTENT_OFFSET = 20;

        private static final int BLOCK_SIZE = 32;

        @Override
        public String digest(String content) {
            try {
                MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
                byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(digest);
            } catch (NoSuchAlgorithmException nae) {
                throw new RuntimeException(nae);
            }
        }

        @Override
        public MessageWrapper decrypt(String key, String ciphertext) {
            try {
                byte[] aesKey = Base64.getDecoder().decode(key);
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM); // PKCS7Padding require bouncycastle, mix ok, encrypt except 'pad block corrupted'
                SecretKeySpec keySpec = new SecretKeySpec(aesKey, KEY_ALGORITHM);
                IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, OFFSET));
                cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
                byte[] data = Base64.getDecoder().decode(ciphertext);
                byte[] plainContainer = cipher.doFinal(data);
                byte[] networkOrder = Arrays.copyOfRange(plainContainer, OFFSET, CONTENT_OFFSET);
                int xmlLength = 0;
                for(byte b : networkOrder) {
                    xmlLength <<= 8;
                    xmlLength |= (b & 0xFF);
                }
                int paddingLength = plainContainer[plainContainer.length - 1];
                if (paddingLength < 1 || paddingLength > BLOCK_SIZE) { // should not here
                    paddingLength = 0;
                }
                String xmlContent = new String(Arrays.copyOfRange(plainContainer, CONTENT_OFFSET, CONTENT_OFFSET + xmlLength), StandardCharsets.UTF_8);
                String appId = new String(Arrays.copyOfRange(plainContainer, CONTENT_OFFSET + xmlLength, plainContainer.length - paddingLength), StandardCharsets.UTF_8);
                return new MessageWrapper(appId, xmlContent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String encrypt(String key, byte[] plaintext) {
            int paddingLength = BLOCK_SIZE - (plaintext.length % BLOCK_SIZE);
            if (paddingLength == 0) {
                paddingLength = BLOCK_SIZE;
            }
            byte[] padded = new byte[plaintext.length + paddingLength];
            System.arraycopy(plaintext, 0, padded, 0, plaintext.length);
            for (int i = plaintext.length; i < plaintext.length + paddingLength; i++) {
                padded[i++] = (byte) (paddingLength & 0xFF);
            }

            try {
                byte[] aesKey = Base64.getDecoder().decode(key);
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                SecretKeySpec keySpec = new SecretKeySpec(aesKey, KEY_ALGORITHM);
                IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, OFFSET));
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
                byte[] encrypted = cipher.doFinal(padded);
                return Base64.getEncoder().encodeToString(encrypted);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    record MessageWrapper(String appId, String content) {
    }
}

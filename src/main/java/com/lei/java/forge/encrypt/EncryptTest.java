package com.lei.java.forge.encrypt;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * TODO
 * </p>
 *
 * @author 伍磊
 */
public class EncryptTest {

    private static final String PREFIX = "_UCM_V2_";

    @Test
    public void test() {
        String value = "user??@mail.com";
        System.out.println(encryptData(value));
        System.out.println("old: " + oldEncryptData(value));
        System.out.println(encryptData("_UCM_V2_"));
        System.out.println(encryptData("_UCM_V2"));
        System.out.println(encryptData("_UCM_V2_123123"));
        System.out.println(encryptData("{x"));

        assertEquals(value, decryptIfNeeded(encryptData(value)));
        assertEquals(value, decryptIfNeeded(encryptData(value)));
        assertEquals(value, decryptIfNeeded(oldEncryptData(value)));
        assertEquals("_UCM_V2_123", decryptIfNeeded("_UCM_V2_123"));
        assertEquals(PREFIX, decryptIfNeeded(PREFIX));
        assertEquals("_UCM_V2_123123", decryptIfNeeded(encryptData("_UCM_V2_123123")));
    }

    public static void main(String[] args) {

    }

    /**
     * 加密敏感数据（手机号或邮箱）
     */
    public static String encryptData(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            byte[] encrypted = aesEncrypt(plainText);
            // 使用Java自带的URL安全Base64编码器
            // A-Z, a-z, 0-9, -, _
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
            return PREFIX + encoded;
        } catch (Exception e) {
            return plainText;
        }
    }

    public static String oldEncryptData(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            byte[] encrypted = aesEncrypt(plainText);
            String encoded = Base64.getEncoder().encodeToString(encrypted);
            return "#" + encoded;
        } catch (Exception e) {
            return plainText;
        }
    }

    private static byte[] aesEncrypt(String plainText) {
        return plainText.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 智能解密：基于前缀判断，解密失败返回原始输入
     */
    public static String decryptIfNeeded(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        try {
            // 新格式：_UCM_V2_前缀
            if (input.startsWith(PREFIX)) {
                try {
                    String encodedData = input.substring(PREFIX.length());
                    // 使用Java自带的URL安全Base64解码器
                    byte[] encrypted = Base64.getUrlDecoder().decode(encodedData);
                    // 使用解密出来的再次加密与input进行比较，如果相同则没问题
                    String oriContent = aesDecrypt(encrypted);
                    String s = encryptData(oriContent);
                    if (StringUtils.equals(input, s)) {
                        return oriContent;
                    } else {
                        // 不相同直接走下面的解析
                        System.err.println("解析失败....");
                    }
                } catch (Exception e) {
                    return input;
                }
            }
            // 旧格式：#前缀
            else if (input.startsWith("#")) {
                try {
                    String encodedData = input.substring(1);
                    // 使用Java自带的标准Base64解码器
                    byte[] encrypted = Base64.getDecoder().decode(encodedData);
                    return aesDecrypt(encrypted);
                } catch (Exception e) {
                    return input;
                }
            }

            // 不是加密数据
            return input;
        } catch (Exception e) {
            return input;
        }
    }

    private static String aesDecrypt(byte[] encrypted) {
        return new String(encrypted);
    }

}

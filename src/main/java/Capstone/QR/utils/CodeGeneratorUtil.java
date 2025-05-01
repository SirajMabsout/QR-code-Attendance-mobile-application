package Capstone.QR.utils;

import java.util.UUID;

public class CodeGeneratorUtil {

    public static String generateJoinCode() {
        return UUID.randomUUID().toString().substring(0, 5);
    }
}

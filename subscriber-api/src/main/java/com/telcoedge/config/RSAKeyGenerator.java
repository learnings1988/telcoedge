package com.telcoedge.config;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.security.BasicPermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class RSAKeyGenerator {

    public static void main( String[] args) throws Exception{

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        writeKey("subscriber-api/src/main/resources/keys/public.pem"
        ,"PUBLIC KEY", pair.getPublic().getEncoded());
        writeKey("subscriber-api/src/test/resources/keys/private.pem"
        ,"PRIVATE KEY", pair.getPrivate().getEncoded());

        System.out.println("Keys generated successfully");
    }


    private static void writeKey(String path, String type, byte[] encoded) throws Exception{
        File file = new File(path);
        file.getParentFile().mkdirs();
        try( Writer w = new FileWriter(file)){
            w.write("-----BEGIN " + type + "-----\n");
            w.write(Base64.getMimeEncoder(64,"\n".getBytes()).encodeToString(encoded));
            w.write("\n-----END " + type + "-----\n");
        }
    }
}

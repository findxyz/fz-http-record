package xyz.fz.record.util;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.ConcurrentHashMap;

public class CertUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static Logger LOGGER = LoggerFactory.getLogger(CertUtil.class);

    private static final String CA_HOST = "findwhy.xyz";

    private static final String CA_CERT = "certs/root/ca.cer";

    private static final String CA_KEY = "certs/root/ca.key";

    private static final String CERT_DIR = "certs/hosts/";

    private static ConcurrentHashMap<String, CertGenerateUtil.CertResult> hostCerts = new ConcurrentHashMap<>();

    private static CertGenerateUtil.CertResult CA_CERT_RESULT;

    private static KeyFactory keyFactory;

    private static CertificateFactory certificateFactory;

    static {
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }

        try {
            CertGenerateUtil.CertResult certResult = load(CA_CERT, CA_KEY);
            if (certResult != null) {
                CA_CERT_RESULT = certResult;
            } else {
                CertGenerateUtil.CertResult newCertResult = CertGenerateUtil.generateCaCert(CA_HOST);
                store(newCertResult, CA_CERT, CA_KEY);
                CA_CERT_RESULT = newCertResult;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public static CertGenerateUtil.CertResult fetchCaCert() {
        return CA_CERT_RESULT;
    }

    public static CertGenerateUtil.CertResult fetchCert(String host) throws Exception {
        CertGenerateUtil.CertResult certResult = hostCerts.get(host);
        if (certResult == null) {
            String certPath = CERT_DIR + host + ".cer";
            String keyPath = CERT_DIR + host + ".key";
            CertGenerateUtil.CertResult loadCertResult = load(certPath, keyPath);
            if (loadCertResult != null) {
                certResult = loadCertResult;
            } else {
                CertGenerateUtil.CertResult newCertResult = CertGenerateUtil.generateCert(CA_HOST, host, CA_CERT_RESULT.getPrivateKey());
                store(newCertResult, certPath, keyPath);
                certResult = newCertResult;
            }
            hostCerts.put(host, certResult);
        }
        return certResult;
    }

    private static CertGenerateUtil.CertResult load(String certPath, String keyPath) throws Exception {
        File cert = new File(certPath);
        File key = new File(keyPath);
        if (cert.exists() && key.exists()) {
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(cert));
            byte[] keyBytes = FileUtils.readFileToByteArray(key);
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            return new CertGenerateUtil.CertResult(privateKey, certificate);
        } else {
            return null;
        }
    }

    private static void store(CertGenerateUtil.CertResult certResult, String certPath, String keyPath) throws Exception {
        FileUtils.writeByteArrayToFile(new File(certPath), certResult.getCertificate().getEncoded());
        FileUtils.writeByteArrayToFile(new File(keyPath), certResult.getPrivateKey().getEncoded());
    }

}

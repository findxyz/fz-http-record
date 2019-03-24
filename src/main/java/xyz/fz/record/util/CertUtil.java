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
import java.util.WeakHashMap;

public class CertUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static Logger LOGGER = LoggerFactory.getLogger(CertUtil.class);

    private static final String CA_HOST = "findwhy.xyz";

    private static final String CA_CERT = "certs/root/ca.cer";

    private static final String CA_KEY = "certs/root/ca.key";

    private static final WeakHashMap<String, CertGenerateUtil.CertResult> HOSTS_CERTS = new WeakHashMap<>();

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
            File caCert = new File(CA_CERT);
            File caKey = new File(CA_KEY);
            if (caCert.exists() && caKey.exists()) {
                X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(caCert));
                byte[] keyBytes = FileUtils.readFileToByteArray(caKey);
                PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytes);
                PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
                CA_CERT_RESULT = new CertGenerateUtil.CertResult(privateKey, certificate);
            } else {
                CertGenerateUtil.CertResult newCertResult = CertGenerateUtil.generateCaCert(CA_HOST);
                FileUtils.writeByteArrayToFile(new File(CA_CERT), newCertResult.getCertificate().getEncoded());
                FileUtils.writeByteArrayToFile(new File(CA_KEY), newCertResult.getPrivateKey().getEncoded());
                CA_CERT_RESULT = newCertResult;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public static synchronized CertGenerateUtil.CertResult fetchCert(String host) throws Exception {
        CertGenerateUtil.CertResult certResult = HOSTS_CERTS.get(host);
        if (certResult == null) {
            certResult = CertGenerateUtil.generateCert(CA_HOST, host, CA_CERT_RESULT.getPrivateKey());
            HOSTS_CERTS.put(host, certResult);
        }
        return certResult;
    }
}

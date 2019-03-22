package xyz.fz.record.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CertGenerateUtil {

    private static SnowFlake snowFlake = new SnowFlake(9, 19);

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.genKeyPair();
    }

    public static CertResult generateCaCert(String caHost) throws Exception {
        KeyPair caKeyPair = generateKeyPair();
        JcaX509v3CertificateBuilder certBuilder = generateCertBuilder(caHost, caHost, caKeyPair);
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caKeyPair.getPrivate());
        return new CertResult(
                caKeyPair.getPrivate(),
                new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))
        );
    }

    public static CertResult generateCert(String caHost, String host, PrivateKey caPriKey) throws Exception {
        KeyPair keyPair = generateKeyPair();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);
        return new CertResult(
                keyPair.getPrivate(),
                new JcaX509CertificateConverter().getCertificate(generateCertBuilder(caHost, host, keyPair).build(signer))
        );
    }

    private static JcaX509v3CertificateBuilder generateCertBuilder(String caHost, String host, KeyPair keyPair) throws CertIOException {
        String issuer = "C=CN, ST=HN, L=ZZ, CN=" + caHost;
        String subject = "C=CN, ST=HN, L=ZZ, CN=" + host;
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                new X500Name(issuer),
                BigInteger.valueOf(snowFlake.generateNextId()),
                new Date(),
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(36500)),
                new X500Name(subject),
                keyPair.getPublic()
        );
        certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralName(GeneralName.dNSName, host));
        return certBuilder;
    }

    public static class CertResult {

        private PrivateKey privateKey;

        private X509Certificate certificate;

        CertResult(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public X509Certificate getCertificate() {
            return certificate;
        }
    }
}

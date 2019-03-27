package xyz.fz.record.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CertGenerateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertGenerateUtil.class);

    private static SnowFlake snowFlake = new SnowFlake(9, 19);

    private static final String ISSUER_TEMPLATE = "C=CN, ST=HN, L=ZZ, O=find, OU=why, CN={caHost}";

    private static final String SUBJECT_TEMPLATE = "C=CN, ST=HN, L=ZZ, O=find, OU=why, CN={host}";

    private static KeyPair DEFAULT_KEY_PAIR;

    static {
        try {
            DEFAULT_KEY_PAIR = generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.genKeyPair();
    }

    public static CertResult generateCaCert(String caHost) throws Exception {
        KeyPair caKeyPair = generateKeyPair();
        JcaX509v3CertificateBuilder certBuilder = generateCertBuilder(caHost, caHost, caKeyPair.getPublic());
        JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils();
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, jcaX509ExtensionUtils.createSubjectKeyIdentifier(caKeyPair.getPublic()));
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, jcaX509ExtensionUtils.createAuthorityKeyIdentifier(caKeyPair.getPublic()));
        certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caKeyPair.getPrivate());
        return new CertResult(
                caKeyPair.getPrivate(),
                new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))
        );
    }

    public static CertResult generateCert(String caHost, String host, PrivateKey caPriKey) throws Exception {
        LOGGER.warn("generate cert for host: {}", host);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);
        JcaX509v3CertificateBuilder certBuilder = generateCertBuilder(caHost, host, DEFAULT_KEY_PAIR.getPublic());
        certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[]{new GeneralName(GeneralName.dNSName, host)}));
        return new CertResult(
                DEFAULT_KEY_PAIR.getPrivate(),
                new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))
        );
    }

    private static JcaX509v3CertificateBuilder generateCertBuilder(String caHost, String host, PublicKey publicKey) throws CertIOException {
        String issuer = ISSUER_TEMPLATE.replace("{caHost}", caHost);
        String subject = SUBJECT_TEMPLATE.replace("{host}", host);
        return new JcaX509v3CertificateBuilder(
                new X500Name(issuer),
                BigInteger.valueOf(snowFlake.generateNextId()),
                new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)),
                new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3650)),
                new X500Name(subject),
                publicKey
        );
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

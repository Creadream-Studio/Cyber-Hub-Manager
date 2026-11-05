package com.cyberhub.manager;

import java.io.*;
import java.nio.file.*;
import java.util.Date;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.PBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.PublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.bc.*;

/**
 * GPG (OpenPGP, RFC 4880) file encryption and decryption.
 *
 * Encrypting produces standard OpenPGP messages: AES-256 with SHA-1 integrity
 * protection (MDC), ZIP-compressed literal data, either symmetric (passphrase)
 * or public-key. Binary output goes to name.gpg, armored output to name.asc.
 * The result decrypts with GnuPG (gpg -d file.gpg) and vice versa: files
 * produced by the desktop gpg tool decrypt here as well.
 *
 * Uses Bouncy Castle's lightweight "Bc" operators only, so no JCE provider
 * registration is needed and Android's stripped built-in BC provider never
 * interferes.
 */
public final class GpgTools {
    private GpgTools() {}

    private static final int BUFFER = 64 * 1024;

    /** Symmetric (passphrase) encryption to an OpenPGP message. */
    public static void encryptSymmetric(File src, File dst, char[] passphrase, boolean armor) throws Exception {
        if (passphrase == null || passphrase.length == 0) throw new IOException("请输入加密口令");
        PGPEncryptedDataGenerator gen = new PGPEncryptedDataGenerator(encryptorBuilder());
        gen.addMethod(new BcPBEKeyEncryptionMethodGenerator(passphrase));
        write(src, dst, gen, armor);
    }

    /** Public-key encryption: the recipient decrypts with the matching secret key. */
    public static void encryptPublicKey(File src, File dst, File publicKeyFile, boolean armor) throws Exception {
        PGPPublicKey key = readPublicKey(publicKeyFile);
        PGPEncryptedDataGenerator gen = new PGPEncryptedDataGenerator(encryptorBuilder());
        gen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(key));
        write(src, dst, gen, armor);
    }

    /**
     * Decrypt an OpenPGP message (symmetric or public-key encrypted, armored or
     * binary). Writes the clear data to a sibling file with .gpg/.asc/.pgp
     * stripped (name.out when no such suffix) and returns it.
     */
    public static File decrypt(File src, char[] passphrase, File secretKeyFile) throws Exception {
        String name = src.getName();
        String outName = name.replaceAll("(?i)\\.(gpg|pgp|asc)$", "");
        if (outName.equals(name)) outName = name + ".out";
        File out = FileOps.child(src.getParentFile(), outName);
        if (out.exists()) throw new IOException("输出已存在：" + outName);
        PGPSecretKeyRingCollection secretRings = null;
        if (secretKeyFile != null && secretKeyFile.isFile())
            try (InputStream keyIn = new BufferedInputStream(new FileInputStream(secretKeyFile))) {
                secretRings = new PGPSecretKeyRingCollection(
                        PGPUtil.getDecoderStream(keyIn), new BcKeyFingerprintCalculator());
            }
        File tmp = new File(src.getParentFile(), "." + outName + ".gpgtmp");
        boolean[] wrote = {false};
        try (InputStream in = new BufferedInputStream(new FileInputStream(src))) {
            PGPObjectFactory factory = new PGPObjectFactory(
                    PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
            process(factory, passphrase, secretRings, tmp, wrote);
            if (!wrote[0]) throw new IOException("文件中没有可解密的 OpenPGP 数据");
            Files.move(tmp.toPath(), out.toPath());
        } catch (Exception e) {
            tmp.delete();
            throw e;
        }
        return out;
    }

    /** Walk nested encrypted / compressed packets until literal data is found. */
    private static void process(PGPObjectFactory factory, char[] passphrase,
                                PGPSecretKeyRingCollection rings, File target, boolean[] wrote) throws Exception {
        for (Object o; (o = factory.nextObject()) != null; ) {
            if (o instanceof PGPEncryptedDataList) {
                PGPEncryptedDataList list = (PGPEncryptedDataList) o;
                Exception failure = null;
                boolean done = false;
                for (int i = 0; i < list.size(); i++) {
                    PGPEncryptedData data = list.get(i);
                    InputStream clear = null;
                    try {
                        if (data instanceof PGPPBEEncryptedData && passphrase != null && passphrase.length > 0) {
                            clear = ((PGPPBEEncryptedData) data).getDataStream(
                                    new BcPBEDataDecryptorFactory(passphrase, new BcPGPDigestCalculatorProvider()));
                        } else if (data instanceof PGPPublicKeyEncryptedData && rings != null) {
                            PGPPublicKeyEncryptedData pkd = (PGPPublicKeyEncryptedData) data;
                            PGPSecretKey secret = rings.getSecretKey(pkd.getKeyID());
                            if (secret != null) {
                                PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(
                                        new BcPGPDigestCalculatorProvider()).build(passphrase == null ? new char[0] : passphrase);
                                PGPPrivateKey privateKey = secret.extractPrivateKey(decryptor);
                                clear = pkd.getDataStream(new BcPublicKeyDataDecryptorFactory(privateKey));
                            }
                        }
                    } catch (Exception e) { failure = e; }
                    if (clear != null) {
                        try {
                            process(new PGPObjectFactory(clear, new BcKeyFingerprintCalculator()),
                                    passphrase, rings, target, wrote);
                        } finally { clear.close(); }
                        done = true;
                        break;
                    }
                }
                if (!done) throw new IOException(failure != null
                        ? "解密失败（口令或私钥不匹配）：" + failure.getMessage()
                        : "无法解密：缺少口令或对应的私钥");
            } else if (o instanceof PGPCompressedData) {
                try (InputStream c = ((PGPCompressedData) o).getDataStream()) {
                    process(new PGPObjectFactory(c, new BcKeyFingerprintCalculator()),
                            passphrase, rings, target, wrote);
                }
            } else if (o instanceof PGPLiteralData) {
                try (InputStream data = ((PGPLiteralData) o).getInputStream();
                     OutputStream os = new BufferedOutputStream(new FileOutputStream(target))) {
                    byte[] buf = new byte[BUFFER];
                    for (int n; (n = data.read(buf)) >= 0; ) os.write(buf, 0, n);
                }
                wrote[0] = true;
            }
        }
    }

    /** Serialize: encrypted packet [compressed packet [literal data]]. */
    private static void write(File src, File dst, PGPEncryptedDataGenerator gen, boolean armor) throws Exception {
        try (OutputStream raw = new BufferedOutputStream(new FileOutputStream(dst))) {
            OutputStream out = armor ? new ArmoredOutputStream(raw) : raw;
            try {
                OutputStream encOut = gen.open(out, new byte[BUFFER]);
                try {
                    PGPCompressedDataGenerator comp =
                            new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
                    OutputStream compOut = comp.open(encOut, new byte[BUFFER]);
                    try {
                        PGPLiteralDataGenerator lit = new PGPLiteralDataGenerator();
                        OutputStream litOut = lit.open(compOut, PGPLiteralData.BINARY,
                                src.getName(), new Date(src.lastModified()), new byte[BUFFER]);
                        try (InputStream in = new BufferedInputStream(new FileInputStream(src))) {
                            byte[] buf = new byte[BUFFER];
                            for (int n; (n = in.read(buf)) >= 0; ) litOut.write(buf, 0, n);
                        } finally { litOut.close(); }
                    } finally { compOut.close(); }
                } finally { encOut.close(); }
            } finally { if (armor) out.close(); }
        }
    }

    private static PGPDataEncryptorBuilder encryptorBuilder() {
        return new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256).setWithIntegrityPacket(true);
    }

    /** First encryption-capable, non-revoked key in an exported public key file. */
    private static PGPPublicKey readPublicKey(File file) throws Exception {
        if (!file.isFile()) throw new IOException("公钥文件未找到：" + file.getPath());
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            PGPPublicKeyRingCollection rings = new PGPPublicKeyRingCollection(
                    PGPUtil.getDecoderStream(in), new BcKeyFingerprintCalculator());
            for (PGPPublicKeyRing ring : rings)
                for (PGPPublicKey key : ring)
                    if (key.isEncryptionKey() && !key.isRevoked()) return key;
        }
        throw new IOException("公钥文件中没有可用的加密公钥");
    }
}

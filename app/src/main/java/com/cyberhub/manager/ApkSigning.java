package com.cyberhub.manager;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import com.android.apksig.*;

/** Publishes an APK only after all requested signature schemes verify. */
public final class ApkSigning {
 private ApkSigning() {}
 public static void sign(File source, File destination, InputStream keyStream) throws Exception {
  sign(source,destination,keyStream,true,true,true);
 }
 /** @param v1 启用 V1 (JAR) 方案, @param v2 启用 V2 (Android 7.0+) 方案, @param v3 启用 V3 (Android 9.0+) 方案 */
 public static void sign(File source, File destination, InputStream keyStream, boolean v1, boolean v2, boolean v3) throws Exception {
  if(!v1&&!v2&&!v3)throw new IOException("至少需要启用一种签名方案");
  if(destination.exists())throw new IOException("签名输出已存在");
  File parent=destination.getAbsoluteFile().getParentFile();
  File temporary=File.createTempFile(".verified-sign-",".apk",parent);
  int platform=v1?21:v2?24:28;
  try {
   KeyStore store=KeyStore.getInstance("PKCS12");char[] password="cyberhub-development".toCharArray();store.load(keyStream,password);
   String alias=store.aliases().nextElement();
   ApkSigner.SignerConfig signer=new ApkSigner.SignerConfig.Builder("CyberHubDevelopment",(PrivateKey)store.getKey(alias,password),Collections.singletonList((X509Certificate)store.getCertificate(alias))).build();
   new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(source).setOutputApk(temporary).setV1SigningEnabled(v1).setV2SigningEnabled(v2).setV3SigningEnabled(v3).setMinSdkVersion(platform).build().sign();
   ApkVerifier.Result result=new ApkVerifier.Builder(temporary).setMinCheckedPlatformVersion(platform).build().verify();
   if(!result.isVerified())throw new IOException("APK 签名校验失败："+result.getErrors());
   if(v1&&!result.isVerifiedUsingV1Scheme())throw new IOException("V1 签名校验失败："+result.getErrors());
   if(v2&&!result.isVerifiedUsingV2Scheme())throw new IOException("V2 签名校验失败："+result.getErrors());
   if(v3&&!result.isVerifiedUsingV3Scheme())throw new IOException("V3 签名校验失败："+result.getErrors());
   if(destination.exists())throw new IOException("输出由另一个操作创建");
   FileOps.replace(temporary,destination);
  }finally{temporary.delete();}
 }
}

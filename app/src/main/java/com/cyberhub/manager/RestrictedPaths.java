package com.cyberhub.manager;

import java.io.File;
import java.util.regex.Pattern;

/** Android 11+ 对 /sdcard/Android/data 与 /sdcard/Android/obb 的直读限制。 */
public final class RestrictedPaths {
    private RestrictedPaths(){}
    private static final Pattern P=Pattern.compile("^/(storage/emulated/[0-9]+|sdcard|storage/self/primary|mnt/user/[0-9]+|data/media/[0-9]+)/Android/(data|obb)(/.*)?$");
    private static final Pattern ROOT=Pattern.compile("^/(storage/emulated/[0-9]+|sdcard|storage/self/primary|mnt/user/[0-9]+|data/media/[0-9]+)/Android/(data|obb)/?$");
    /** Android/data 与 Android/obb 的根目录本身通常可直接枚举，子目录才受限制。 */
    public static boolean isRestrictedRoot(File f){
        return ROOT.matcher(f.getAbsolutePath().replace('\\','/')).matches();
    }
    public static boolean isRestricted(File f){
        return isRestricted(f,null);
    }
    public static boolean isRestricted(File f,String ownPackage){
        String p=f.getAbsolutePath().replace('\\','/');
        if(!P.matcher(p).matches())return false;
        if(ownPackage!=null&&!ownPackage.isEmpty()){
            String prefix="/Android/data/"+ownPackage;
            int i=p.indexOf(prefix);
            if(i>=0&&(p.length()==i+prefix.length()||p.charAt(i+prefix.length())=='/'))return false;
        }
        return true;
    }
}

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

public class Utils {
    private final static String BASE_DIR = "..\\src\\filesystem\\";
    private static final int HASH_BITS_SIZE = 256;
    private static final int HASH_ENCODE_SIZE = 64;
    private static final BigInteger CEIL = BigInteger.ONE.shiftLeft(256);
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] encode(String originalString){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeToHex(String originalString){
        return bytesToHex(encode(originalString));
    }

    public static boolean fileExists(String relativePath){
        String filePath = BASE_DIR + relativePath;
        return Files.exists(Paths.get(filePath));
    }

    public static boolean deleteFile(String relativePath){
        String filePath = BASE_DIR + relativePath;
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFileContent(String relativePath){
        String filePath = BASE_DIR + relativePath;
        byte[] encoded = new byte[0];
        try {
            Path path = Paths.get(filePath);
            //Path path = Paths.get(filePath).toAbsolutePath();
            //File file = new File(path.toString());
            //System.out.println("Path: " + path.toString());
            if(Files.exists(path)){
                System.out.println("Exists file at " + path);
            }
            else{
                System.out.println("Doesn't exists file at " + path);
                return new String(encoded, StandardCharsets.UTF_8);
            }
            encoded = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }

    private static BigInteger stringToHex(String originalString){
        return new BigInteger(originalString, 16);
    }

    private static BigInteger hashDistance(String firstString, String secondString){
        BigInteger firstBi = stringToHex(firstString).mod(CEIL);
        BigInteger secondBi = stringToHex(secondString).mod(CEIL);
        BigInteger distance = secondBi.subtract(firstBi);
        if (distance.signum() == -1){
            distance.add(CEIL);
        }
        return distance;
    }

    public static String closestNode(List<String> nodes, String target){
        if (nodes.size() == 0){
            return "";
        }
        String closestNode = nodes.get(0);
        BigInteger bestDistance = hashDistance(target, nodes.get(0));
        for(int i = 1; i < nodes.size(); i++){
            if(bestDistance.signum() == 0) break;
            BigInteger currentDistance = hashDistance(target, nodes.get(i));
            if(currentDistance.compareTo(bestDistance) == -1){
                bestDistance = currentDistance;
                closestNode = nodes.get(i);
            }
        }
        return closestNode;
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static boolean makeDir(String relativePath){
        String dirPath = BASE_DIR + relativePath;
        File dir = new File(dirPath);
        deleteDir(dir);
        return dir.mkdir();
    }

    public static boolean writeToFile(String relativePath, String content, boolean create){
        String filePath = BASE_DIR + relativePath;
        File newFile = new File(filePath);
        try {
            if(create){
                if(!newFile.createNewFile()){
                    return false;
                }
            }
            FileWriter writer = new FileWriter(newFile);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }


    public static int getHashEncodeSize(){
        return HASH_ENCODE_SIZE;
    }
}

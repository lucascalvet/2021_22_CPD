package utils;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Utils {
    private final static String BASE_DIR = "src\\filesystem\\";
    private static final int HASH_BITS_SIZE = 256;
    public static final String MSG_END = "\nEND";
    public static final String MSG_TOMBSTONE = "TOMBSTONE";
    public static final String MSG_END_SERVICE = "END OF SERVICE";
    private static final int HASH_ENCODE_SIZE = 64;
    private static final BigInteger CEIL = BigInteger.ONE.shiftLeft(256);

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] encode(String originalString) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeToHex(String originalString){
        return bytesToHex(encode(originalString.replaceAll("\r", "")));
    }

    public static boolean fileExists(String relativePath) {
        String filePath = BASE_DIR + relativePath;
        return Files.exists(Paths.get(filePath));
    }

    public static boolean deleteFile(String relativePath) {
        String filePath = BASE_DIR + relativePath;
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFileContent(String relativePath) {
        String filePath = BASE_DIR + relativePath;
        byte[] encoded = new byte[0];
        try {
            Path path = Paths.get(filePath);
            //Path path = Paths.get(filePath).toAbsolutePath();
            //File file = new File(path.toString());
            //System.out.println("Path: " + path.toString());
            if (Files.exists(path)) {
                System.out.println("Exists file at " + path);
            } else {
                System.out.println("Doesn't exists file at " + path);
                return new String(encoded, StandardCharsets.UTF_8);
            }
            encoded = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }

    private static BigInteger stringToHex(String originalString) {
        return new BigInteger(originalString, 16);
    }

    private static BigInteger hashDistance(String firstString, String secondString) {
        BigInteger firstBi = stringToHex(firstString).mod(CEIL);
        BigInteger secondBi = stringToHex(encodeToHex(secondString)).mod(CEIL);
        BigInteger distance = secondBi.subtract(firstBi);
        if (distance.signum() == -1) {
            distance.add(CEIL);
        }
        return distance;
    }

    private static Comparator<String> compareHashDistance(String target) {
        return Comparator.comparing((String node) -> hashDistance(target, node));
    }

    public static List<String> getActiveMembersSorted(String hashedId, String hashedTarget) {
        List<String> activeNodes = getActiveMembers(hashedId);
        activeNodes.sort(compareHashDistance(hashedTarget));
        return activeNodes;
    }

    public static String closestNode(List<String> nodes, String target) {
        if (nodes.size() == 0) {
            return "";
        }
        String closestNode = nodes.get(0);
        BigInteger bestDistance = hashDistance(target, nodes.get(0));
        for (int i = 1; i < nodes.size(); i++) {
            if (bestDistance.signum() == 0) break;
            BigInteger currentDistance = hashDistance(target, nodes.get(i));
            if (currentDistance.compareTo(bestDistance) == -1) {
                bestDistance = currentDistance;
                closestNode = nodes.get(i);
            }
        }
        return closestNode;
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static boolean makeDir(String relativePath) {
        String dirPath = BASE_DIR + relativePath;
        File dir = new File(dirPath);
        deleteDir(dir);
        return dir.mkdir();
    }

    public static boolean writeToFile(String relativePath, String content, boolean create) {
        String filePath = BASE_DIR + relativePath;
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current absolute path is: " + s);
        System.out.println("FP: " + filePath);
        File newFile = new File(filePath);
        try {
            if (create) {
                if (!newFile.createNewFile()) {
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

    public static String getNLogLines(String hashedId, int n) {
        String line;
        StringBuilder logs = new StringBuilder();
        File membershipLog = new File(BASE_DIR + hashedId + "\\membership_log.txt");
        try {
            FileReader fr = new FileReader(membershipLog);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null && n > 0) {
                logs.append(line).append("\n");
                n--;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return logs.toString();
    }

    public static Map<String, Integer> readLogs(String hashedId) {
        Map<String, Integer> nodes = new HashMap<String, Integer>();
        String line;
        File membershipLog = new File(BASE_DIR + hashedId + "\\membership_log.txt");
        try {
            FileReader fr = new FileReader(membershipLog);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                String[] splited = line.split("\\s+");
                if (splited.length == 2) {
                    nodes.put(splited[0], Integer.valueOf(splited[1]));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return nodes;
    }

    public static List<String> getActiveMembers(String hashedId) {
        Map<String, Integer> nodes = readLogs(hashedId);
        List<String> activeNodes = new ArrayList<>();
        for (Map.Entry<String, Integer> node : nodes.entrySet()) {
            if(!activeNodes.contains(node) && node.getValue() % 2 == 0){
                activeNodes.add(node.getKey());
            }
        }
        return activeNodes;
    }

    public static int getHashEncodeSize() {
        return HASH_ENCODE_SIZE;
    }

    public static int updateLogs(String nodeId, Integer counter, String hashedId) {
        Map<String, Integer> logs = readLogs(hashedId);
        File logFile = new File(BASE_DIR + hashedId + "\\membership_log.txt");

        // open file
        FileWriter writer = null;

        // check if log already exists, and if it does, update the counter
        if (!logs.isEmpty()) {
            if (logs.containsKey(nodeId)) {
                if (!logs.get(nodeId).equals(counter)) {
                    logs.put(nodeId, counter);
                }

                try {
                    writer = new FileWriter(logFile, false);

                    FileWriter finalWriter = writer;
                    logs.forEach((key, value) -> {
                        String log = key + " " + value + "\n";
                        try {
                            finalWriter.write(log);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // if it does not exist, add log
        else {
            String log = nodeId + " " + counter;
            try {
                writer = new FileWriter(logFile, true);
                writer.write(log);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }
}

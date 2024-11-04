package com.lz.blockchainauthentication.POJO;

import java.util.ArrayList; // 导入 ArrayList 类
import java.util.List; // 导入 List 接口
import java.util.Random; // 导入 Random 类


public class MerkleTreeGenerator {
    private List<String> accessList;
    private int n;
    private long seed;

    public long getSeed() {
        return seed;
    }

    public MerkleTreeGenerator(List<String> accessList) {

        this.accessList = accessList;
        this.n = accessList.size();
        this.seed = new Random().nextLong();
    }

    public String generateMerkleRoot() {
        List<String> leafNodes = new ArrayList<>(n);
        Random random = new Random(seed);
        for (int i = 0; i < n; i++) {

            String leafNode = String.valueOf(random.nextInt() + Long.parseLong(accessList.get(i)));
            leafNodes.add(leafNode);
        }
        return constructMerkleTree(leafNodes);
    }

    private String constructMerkleTree(List<String> items) {
        if (items.size() == 1) {
            return hash(items.get(0));
        }
        List<String> parentLevel = new ArrayList<>();
        for (int i = 0; i < items.size(); i += 2) {
            String left = items.get(i);
            String right = i + 1 < items.size() ? items.get(i + 1) : "";
            parentLevel.add(hash(left + right));
        }
        return constructMerkleTree(parentLevel);
    }

    private String hash(String data) {

        return String.valueOf(data.hashCode());
    }
}


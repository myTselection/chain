package io.pivotal.cf.chain.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.pivotal.cf.chain.Hasher;
import org.springframework.http.HttpStatus;

@JsonPropertyOrder({"hash", "leaves", "height", "root"})
public class MerkleTree extends AbstractChain {

    public MerkleTree(Hasher hasher) {
        super(hasher);
    }

    public String addEntry(String entry) throws VerificationException {
        if(leaves() >= MAX_MERKLE_ENTRIES) {
            throw new VerificationException("tree max size: " + MAX_MERKLE_ENTRIES + " reached.", HttpStatus.INSUFFICIENT_STORAGE);
        }

        //create a leaf for the entry
        Leaf leaf = new Leaf(getHasher().createId(), getHasher().hash(entry));

        //add the leaf to the tree
        addLeaf(leaf);

        //calculate hashes from newly added node up to root
        rehash(leaf);

        return leaf.getKey();
    }

    private void addLeaf(Leaf l) {
        char[] path = calcPath(leaves());
        buildPath(path);
        addLeaf(path, l);
    }

    private void addLeaf(char[] path, Leaf l) {
        Node parent = getRoot();

        if (leaves() > 0) {
            for (char c : path) {
                if (c == '0') {
                    parent = (Node) parent.getLeft();
                }
                if (c == '1') {
                    parent = (Node) parent.getRight();
                }
            }
        }

        parent.addChild(l);
        put(l);
    }

    private void put(Leaf l) {
        getLeaves().put(l.getKey(), l);
    }

    private void buildPath(char[] path) {
        //special case: no entries yet, the root  will be the parent
        if (leaves() < 1) {
            return;
        }

        //if tree is full we need a new root to start from
        if (treeIsFull()) {
            createNewRoot();
        }

        Node parent = getRoot();
        //start at root and build out the branch as needed
        for (char c : path) {
            if (c == '0') {
                if (parent.getLeft() == null) {
                    Node left = new Node();
                    parent.addChild(left);
                }
                parent = (Node) parent.getLeft();
            }

            if (c == '1') {
                if (parent.getRight() == null) {
                    Node right = new Node();
                    parent.addChild(right);
                }
                parent = (Node) parent.getRight();
            }
        }
    }

    private void createNewRoot() {
        Node n = new Node();
        n.addChild(getRoot());
        setRoot(n);
    }

    private boolean treeIsFull() {
        return isPowerOf2(leaves());
    }

    public void loadRandomEntries(int numberOfEntries) throws VerificationException {
        for (int i = 0; i < numberOfEntries; i++) {
            addEntry(getHasher().createId());
        }
    }

    private boolean isPowerOf2(int i) {
        return (i != 0) && ((i & (i - 1)) == 0);
    }

    @JsonProperty
    int height() {
        if (leaves() <= 0) {
            return 0;
        }

        //calculate base2 log
        double d = Math.log(leaves()) / Math.log(2);

        //round up to next whole number
        return (int) Math.ceil(d);
    }

    char[] calcPath(int leaves) {
        return Integer.toBinaryString(leaves).toCharArray();
    }

}
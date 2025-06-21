package autocompletion;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {

    private Character value;
    private Map<Character, TrieNode> children;
    private boolean isEndOfWord;

    TrieNode(Character value) {
        this.value = value;
        this.children = new HashMap<>();
        this.isEndOfWord = false;
    }

    public TrieNode addChild(TrieNode node) {
        children.put(node.value, node);
        return node;
    }

    public TrieNode addChild(Character value) {
        TrieNode node = new TrieNode(value);
        return addChild(node);
    }

    public Character getValue() { return this.value; }

    public TrieNode getChild(Character c) { return this.children.get(c); }

    public void setIsEndOfWord() { isEndOfWord = true; }

    public boolean getIsEndOfWord() { return isEndOfWord; }

    public Map<Character, TrieNode> getChildren() { return children; }

}

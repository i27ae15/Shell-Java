package autocompletion;

import java.util.ArrayList;
import java.util.Map;

public class Trie {

    private final String[] BUILT_IN_WORDS = new String[]{"echo", "exit"};
    private TrieNode root;

    public Trie() {

        try {
            buildTrie();
        } catch (IllegalAccessException e) {
            System.err.println("THIS TRIE HAS ALREADY BEEN BUILT");
            return;
        }

    }

    private void buildTrie() throws IllegalAccessException {

        if (root != null) throw new IllegalAccessException("This trie has already been built");

        root = new TrieNode('*');

        for (String word : BUILT_IN_WORDS) {
            addWord(word);
        }

    }

    public void addWord(String word) {

        TrieNode currentNode = root;
        int idx = 0;

        while (word.length() > idx) {

            TrieNode nextNode = currentNode.getChild(word.charAt(idx));

            if (nextNode != null) {
                currentNode = nextNode;
            } else {
                // Create a new child with the current character
                currentNode = currentNode.addChild(word.charAt(idx));
            }

            idx++;

        }

        currentNode.setIsEndOfWord();

    }

    public TrieNode search(String word) {

        TrieNode currentNode = root;
        int idx = 0;

        while (word.length() > idx) {

            TrieNode nextNode = currentNode.getChild(word.charAt(idx));

            if (nextNode == null) return null;
            // utils.Printer.println("NODE_VALUE: " + nextNode.getValue());

            currentNode = nextNode;
            idx++;

        }

        return currentNode;

    }

    public ArrayList<String> getPossibleOptions(String word) {

        ArrayList<String> possibleOptions = new ArrayList<>();

        // First search to get to the node
        TrieNode node = search(word);

        // perform a search to get to the last node that we can return
        if (node != null) dfs(node, word, possibleOptions);

        return possibleOptions;

    }

    private void dfs(TrieNode node, String word, ArrayList<String> possibleOptions) {

        Map<Character, TrieNode> childrenNode = node.getChildren();

        for (Map.Entry<Character, TrieNode> entry : childrenNode.entrySet()) {

            Character ch = entry.getKey();
            TrieNode child = entry.getValue();

            word += ch;

            if (child.getIsEndOfWord()) possibleOptions.add(word);
            dfs(child, word, possibleOptions);

        }

    }

}

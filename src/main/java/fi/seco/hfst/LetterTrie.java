package fi.seco.hfst;

import com.carrotsearch.hppc.CharIntHashMap;
import com.carrotsearch.hppc.CharObjectHashMap;

public class LetterTrie {
	public class LetterTrieNode {
		private final CharIntHashMap symbols;
		private final CharObjectHashMap<LetterTrieNode> children;

		public void addString(String str, int symbolNumber) {
			if (str.length() > 1) {
				if (children.containsKey(str.charAt(0)) == false) children.put(str.charAt(0), new LetterTrieNode());
				children.get(str.charAt(0)).addString(str.substring(1, str.length()), symbolNumber);
			} else if (str.length() == 1) symbols.put(str.charAt(0), symbolNumber);
		}

		public int findKey(IndexString string) {
			if (string.index >= string.str.length()) return HfstOptimizedLookup.NO_SYMBOL_NUMBER;
			Character at_s = string.str.charAt(string.index);
			++string.index;
			LetterTrieNode child = children.get(at_s);
			if (child==null) {
				int symbol = symbols.get(at_s);
				if (symbol==0) {
					--string.index;
					return HfstOptimizedLookup.NO_SYMBOL_NUMBER;
				}
				return symbol;
			}
			int s = child.findKey(string);
			if (s == HfstOptimizedLookup.NO_SYMBOL_NUMBER) {
				int symbol = symbols.get(at_s);
				if (symbol==0) {
					--string.index;
					return HfstOptimizedLookup.NO_SYMBOL_NUMBER;
				}
				return symbol;
			}
			return s;
		}

		public LetterTrieNode() {
			symbols = new CharIntHashMap();
			children = new CharObjectHashMap<LetterTrieNode>();
		}
	}

	private final LetterTrieNode root;

	public LetterTrie() {
		root = new LetterTrieNode();
	}

	public void addString(String str, int symbolNumber) {
		root.addString(str, symbolNumber);
	}

	int findKey(IndexString str) {
		return root.findKey(str);
	}
}

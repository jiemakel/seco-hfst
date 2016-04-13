package fi.seco.hfst;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectMap;

/**
 * Reads the header, alphabet, index table and transition table and provides
 * interfaces to them.
 */
public class UnweightedTransducer implements Transducer {

	/**
	 * On instantiation reads the transducer's index table and provides an
	 * interface to it.
	 */
	public static class IndexTable {
		private final int[] ti_inputSymbols;
		private final long[] ti_targets;

		public int getInput(int i) {
			return ti_inputSymbols[i];
		}

		public long getTarget(int i) {
			return ti_targets[i];
		}

		public boolean isFinal(int i) {
			return (ti_inputSymbols[i] == HfstOptimizedLookup.NO_SYMBOL_NUMBER && ti_targets[i] != HfstOptimizedLookup.NO_TABLE_INDEX);
		}

		public IndexTable(DataInputStream input, int indicesCount) throws java.io.IOException {
			ByteArray b = new ByteArray(indicesCount * 6);
			input.readFully(b.getBytes());
			// each index entry is a unsigned short followed by an unsigned int
			ti_inputSymbols = new int[indicesCount];
			ti_targets = new long[indicesCount];

			int i = 0;
			while (i < indicesCount) {
				ti_inputSymbols[i] = b.getUShort();
				ti_targets[i] = b.getUInt();
				i++;
			}
		}

	}

	/**
	 * On instantiation reads the transducer's transition table and provides an
	 * interface to it.
	 */
	public static class TransitionTable {
		private final int[] ti_inputSymbols;
		private final int[] ti_outputSymbols;
		private final long[] ti_targets;

		public TransitionTable(DataInputStream input, int transitionCount) throws java.io.IOException {
			ByteArray b = new ByteArray(transitionCount * 8);
			// each transition entry is two unsigned shorts and an unsigned int
			input.readFully(b.getBytes());
			ti_inputSymbols = new int[transitionCount];
			ti_outputSymbols = new int[transitionCount];
			ti_targets = new long[transitionCount];
			int i = 0;
			while (i < transitionCount) {
				ti_inputSymbols[i] = b.getUShort();
				ti_outputSymbols[i] = b.getUShort();
				ti_targets[i] = b.getUInt();
				i++;
			}
		}

		public boolean matches(int pos, int symbol) {
			if (ti_inputSymbols[pos] == HfstOptimizedLookup.NO_SYMBOL_NUMBER) return false;
			if (symbol == HfstOptimizedLookup.NO_SYMBOL_NUMBER) return true;
			return (ti_inputSymbols[pos] == symbol);
		}

		public int getInput(int pos) {
			return ti_inputSymbols[pos];
		}

		public int getOutput(int pos) {
			return ti_outputSymbols[pos];
		}

		public long getTarget(int pos) {
			return ti_targets[pos];
		}

		public boolean isFinal(int pos) {
			return (ti_inputSymbols[pos] == HfstOptimizedLookup.NO_SYMBOL_NUMBER && ti_outputSymbols[pos] == HfstOptimizedLookup.NO_SYMBOL_NUMBER && ti_targets[pos] == 1);
		}

		public int size() {
			return ti_targets.length;
		}

	}

	protected TransducerHeader header;
	protected TransducerAlphabet alphabet;
	protected IntObjectMap<FlagDiacriticOperation> operations;
	protected LetterTrie letterTrie;
	protected IndexTable indexTable;
	protected TransitionTable transitionTable;

	private class State {
		protected Stack<int[]> stateStack;
		protected ArrayList<Result> displayVector;
		protected IntArrayList outputString;
		protected IntArrayList inputString;
		protected int outputPointer;
		protected int inputPointer;

		public State(String input) {
			stateStack = new Stack<int[]>();
			int[] neutral = new int[alphabet.features];
			for (int i = 0; i < neutral.length; ++i)
				neutral[i] = 0;
			stateStack.push(neutral);
			outputString = new IntArrayList();
			inputString = new IntArrayList();
			outputPointer = 0;
			inputPointer = 0;
			displayVector = new ArrayList<Result>();

			IndexString inputLine = new IndexString(input);
			while (inputLine.index < input.length()) {
				inputString.add(letterTrie.findKey(inputLine));
				if (inputString.get(inputString.size() - 1) == HfstOptimizedLookup.NO_SYMBOL_NUMBER) {
					inputString.clear();
					break;
				}
			}
			inputString.add(HfstOptimizedLookup.NO_SYMBOL_NUMBER);

		}
	}

	public UnweightedTransducer(DataInputStream input, TransducerHeader h, TransducerAlphabet a) throws java.io.IOException {
		header = h;
		alphabet = a;
		operations = alphabet.operations;
		letterTrie = new LetterTrie();
		int i = 0;
		while (i < header.getInputSymbolCount()) {
			letterTrie.addString(alphabet.keyTable.get(i), i);
			i++;
		}
		indexTable = new IndexTable(input, header.getIndexTableSize());
		transitionTable = new TransitionTable(input, header.getTargetTableSize());
	}

	private int pivot(long i) {
		if (i >= HfstOptimizedLookup.TRANSITION_TARGET_TABLE_START)
			return (int) (i - HfstOptimizedLookup.TRANSITION_TARGET_TABLE_START);
		return (int) i;
	}

	private void tryEpsilonIndices(int index, State state) {
		if (indexTable.getInput(index) == 0) tryEpsilonTransitions(pivot(indexTable.getTarget(index)), state);
	}

	private void tryEpsilonTransitions(int index, State state) {
		while (true)
			// first test for flag
			if (operations.containsKey(transitionTable.getInput(index))) {
				if (!pushState(operations.get(transitionTable.getInput(index)), state)) {
					++index;
					continue;
				} else {
					if (state.outputPointer==state.outputString.size()) state.outputString.add(transitionTable.getOutput(index));
					else state.outputString.set(state.outputPointer,transitionTable.getOutput(index));
					++state.outputPointer;
					getAnalyses(transitionTable.getTarget(index), state);
					--state.outputPointer;
					++index;
					state.stateStack.pop();
					continue;
				}
			} else if (transitionTable.getInput(index) == 0) { // epsilon transitions
				if (state.outputPointer==state.outputString.size()) state.outputString.add(transitionTable.getOutput(index));
				else state.outputString.set(state.outputPointer,transitionTable.getOutput(index));
				++state.outputPointer;
				getAnalyses(transitionTable.getTarget(index), state);
				--state.outputPointer;
				++index;
				continue;
			} else break;
	}

	private void findIndex(int index, State state) {
		if (indexTable.getInput(index + (state.inputString.get(state.inputPointer - 1))) == state.inputString.get(state.inputPointer - 1))
			findTransitions(pivot(indexTable.getTarget(index + state.inputString.get(state.inputPointer - 1))), state);
	}

	private void findTransitions(int index, State state) {
		while (transitionTable.getInput(index) != HfstOptimizedLookup.NO_SYMBOL_NUMBER) {
			if (transitionTable.getInput(index) == state.inputString.get(state.inputPointer - 1)) {
				if (state.outputPointer==state.outputString.size()) state.outputString.add(transitionTable.getOutput(index));
				else state.outputString.set(state.outputPointer,transitionTable.getOutput(index));
				++state.outputPointer;
				getAnalyses(transitionTable.getTarget(index), state);
				--state.outputPointer;
			} else return;
			++index;
		}
	}

	private void getAnalyses(long idx, State state) {
		if (idx >= HfstOptimizedLookup.TRANSITION_TARGET_TABLE_START) {
			int index = pivot(idx);
			tryEpsilonTransitions(index + 1, state);
			if (state.inputString.get(state.inputPointer) == HfstOptimizedLookup.NO_SYMBOL_NUMBER) { // end of input string
				if (state.outputPointer==state.outputString.size()) state.outputString.add(HfstOptimizedLookup.NO_SYMBOL_NUMBER);
				else state.outputString.set(state.outputPointer,HfstOptimizedLookup.NO_SYMBOL_NUMBER);
				if (transitionTable.size() > index && transitionTable.isFinal(index)) noteAnalysis(state);
				return;
			}
			++state.inputPointer;
			findTransitions(index + 1, state);
		} else {
			int index = pivot(idx);
			tryEpsilonIndices(index + 1, state);
			if (state.inputString.get(state.inputPointer) == HfstOptimizedLookup.NO_SYMBOL_NUMBER) { // end of input string
				if (state.outputPointer==state.outputString.size()) state.outputString.add(HfstOptimizedLookup.NO_SYMBOL_NUMBER);
				else state.outputString.set(state.outputPointer,HfstOptimizedLookup.NO_SYMBOL_NUMBER);
				if (indexTable.isFinal(index)) noteAnalysis(state);
				return;
			}
			++state.inputPointer;
			findIndex(index + 1, state);
		}
		--state.inputPointer;
		if (state.outputPointer==state.outputString.size()) state.outputString.add(HfstOptimizedLookup.NO_SYMBOL_NUMBER);
		else state.outputString.set(state.outputPointer,HfstOptimizedLookup.NO_SYMBOL_NUMBER);
	}

	private List<String> getSymbols(State state) {
		int i = 0;
		List<String> symbols = new ArrayList<String>();
		while (i<state.outputString.size() && state.outputString.get(i) != HfstOptimizedLookup.NO_SYMBOL_NUMBER)
			symbols.add(alphabet.keyTable.get(state.outputString.get(i++)));
		return symbols;
	}
	
	private void noteAnalysis(State state) {
		state.displayVector.add(new Result(getSymbols(state), 1.0f));
	}

	@Override
	public List<Result> analyze(String input) {
		State state = new State(input);
		if (state.inputString.get(0)==HfstOptimizedLookup.NO_SYMBOL_NUMBER) return Collections.emptyList();
		getAnalyses(0, state);
		return state.displayVector;
	}
	
	@Override
	public List<String> getAlphabet() {
		return alphabet.keyTable;
	}

	private boolean pushState(FlagDiacriticOperation flag, State state) {
		int[] top = new int[alphabet.features];
		System.arraycopy(state.stateStack.peek(), 0, top, 0, alphabet.features);
		if (flag.op == HfstOptimizedLookup.FlagDiacriticOperator.P) { // positive set
			state.stateStack.push(top);
			state.stateStack.peek()[flag.feature] = flag.value;
			return true;
		} else if (flag.op == HfstOptimizedLookup.FlagDiacriticOperator.N) { // negative set
			state.stateStack.push(top);
			state.stateStack.peek()[flag.feature] = -1 * flag.value;
			return true;
		} else if (flag.op == HfstOptimizedLookup.FlagDiacriticOperator.R) { // require
			if (flag.value == 0) // empty require
			{
				if (state.stateStack.peek()[flag.feature] == 0)
					return false;
				else {
					state.stateStack.push(top);
					return true;
				}
			} else if (state.stateStack.peek()[flag.feature] == flag.value) {
				state.stateStack.push(top);
				return true;
			}
			return false;
		} else if (flag.op == HfstOptimizedLookup.FlagDiacriticOperator.D) { // disallow
			if (flag.value == 0) // empty disallow
			{
				if (state.stateStack.peek()[flag.feature] != 0)
					return false;
				else {
					state.stateStack.push(top);
					return true;
				}
			} else if (state.stateStack.peek()[flag.feature] == flag.value) return false;
			state.stateStack.push(top);
			return true;
		} else if (flag.op == HfstOptimizedLookup.FlagDiacriticOperator.C) { // clear
			state.stateStack.push(top);
			state.stateStack.peek()[flag.feature] = 0;
			return true;
		} else if (flag.op == HfstOptimizedLookup.FlagDiacriticOperator.U) { // unification
			if ((state.stateStack.peek()[flag.feature] == 0) || (state.stateStack.peek()[flag.feature] == flag.value) || (state.stateStack.peek()[flag.feature] != flag.value && state.stateStack.peek()[flag.feature] < 0)) {

				state.stateStack.push(top);
				state.stateStack.peek()[flag.feature] = flag.value;
				return true;
			}
			return false;
		}
		return false; // compiler sanity
	}

}
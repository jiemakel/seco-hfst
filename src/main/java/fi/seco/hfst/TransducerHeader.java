package fi.seco.hfst;

import java.io.InputStream;

/**
 * On instantiation reads the transducer's header and provides an interface to
 * it.
 */
public class TransducerHeader {
	private final int number_of_input_symbols;
	private final int number_of_symbols;
	private final int size_of_transition_index_table;
	private final int size_of_transition_target_table;
	private final int number_of_states;
	private final int number_of_transitions;

	private final boolean weighted;
	private final boolean deterministic;
	private final boolean input_deterministic;
	private final boolean minimized;
	private final boolean cyclic;
	private final boolean has_epsilon_epsilon_transitions;
	private final boolean has_input_epsilon_transitions;
	private final boolean has_input_epsilon_cycles;
	private final boolean has_unweighted_input_epsilon_cycles;

	private boolean hfst3;
	private final boolean intact;

	/**
	 * Read in the (56 bytes of) header information, which unfortunately is
	 * mostly in little-endian unsigned form.
	 */
	public TransducerHeader(InputStream input) throws java.io.IOException {
		hfst3 = false;
		intact = true; // could add some checks to toggle this and check outside
		ByteArray head = new ByteArray(5);
		input.read(head.getBytes());
		if (begins_hfst3_header(head)) {
			skip_hfst3_header(input);
			input.read(head.getBytes());
			hfst3 = true;
		}
		ByteArray b = new ByteArray(head, 56);
		input.read(b.getBytes(), 5, 51);

		number_of_input_symbols = b.getUShort();
		number_of_symbols = b.getUShort();
		size_of_transition_index_table = (int) b.getUInt();
		size_of_transition_target_table = (int) b.getUInt();
		number_of_states = (int) b.getUInt();
		number_of_transitions = (int) b.getUInt();

		weighted = b.getBool();
		deterministic = b.getBool();
		input_deterministic = b.getBool();
		minimized = b.getBool();
		cyclic = b.getBool();
		has_epsilon_epsilon_transitions = b.getBool();
		has_input_epsilon_transitions = b.getBool();
		has_input_epsilon_cycles = b.getBool();
		has_unweighted_input_epsilon_cycles = b.getBool();
	}

	public boolean begins_hfst3_header(ByteArray bytes) {
		if (bytes.getSize() < 5) return false;
		// HFST\0
		return (bytes.getUByte() == 72 && bytes.getUByte() == 70 && bytes.getUByte() == 83 && bytes.getUByte() == 84 && bytes.getUByte() == 0);
	}

	public void skip_hfst3_header(InputStream file) throws java.io.IOException {
		ByteArray len = new ByteArray(2);
		file.read(len.getBytes());
		file.skip(len.getUShort() + 1);
	}

	public int getInputSymbolCount() {
		return number_of_input_symbols;
	}

	public int getSymbolCount() {
		return number_of_symbols;
	}

	public int getIndexTableSize() {
		return size_of_transition_index_table;
	}

	public int getTargetTableSize() {
		return size_of_transition_target_table;
	}

	public boolean isWeighted() {
		return weighted;
	}

	public boolean hasHfst3Header() {
		return hfst3;
	}

	public boolean isIntact() {
		return intact;
	}
}

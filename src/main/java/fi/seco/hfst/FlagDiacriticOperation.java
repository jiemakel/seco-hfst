package fi.seco.hfst;

/**
 * A representation of one flag diacritic statement
 */
public class FlagDiacriticOperation {
	public HfstOptimizedLookup.FlagDiacriticOperator op;
	public int feature;
	public int value;

	public FlagDiacriticOperation(HfstOptimizedLookup.FlagDiacriticOperator operation, int feat, int val) {
		op = operation;
		feature = feat;
		value = val;
	}

	public FlagDiacriticOperation() {
		op = HfstOptimizedLookup.FlagDiacriticOperator.P;
		feature = HfstOptimizedLookup.NO_SYMBOL_NUMBER;
		value = 0;
	}

	public boolean isFlag() {
		return feature != HfstOptimizedLookup.NO_SYMBOL_NUMBER;
	}
}

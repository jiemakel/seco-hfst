package fi.seco.hfst;

import java.util.List;

public interface Transducer {
	public List<Result> analyze(String str);

	public static final class Result {
		private final List<String> symbols;
		private final float weight;

		public Result(List<String> symbols, float weight) {
			this.symbols = symbols;
			this.weight = weight;
		}

		public final List<String> getSymbols() {
			return symbols;
		}

		public final float getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			return symbols + ": " + weight;
		}
	}
}

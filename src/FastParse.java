import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastParse {
	private Map<String, Double> varMap = new HashMap<String, Double>();
	private Map<String, Op> opMap = new HashMap<String, Op>();

	public static void main(String[] args) {
		FastParse p = new FastParse();
		ParseNode pn = p.Parse("t/t*2");
		p.SetVariable("t", 1);
		p.SetVariable("p3", 1);
		p.SetVariable("p1", 1);
		p.SetVariable("p2", 1);

		int p1 = 1;
		int p2 = 1;
		int p3 = 1;

		for (int t = 1; t < 1000000; ++t) {
			p.SetVariable("t", t);
			int r2 = (int) pn.Eval();
			int r = (int) t / t * 2;
			if (r != r2) {
				System.out.println(t + " : " + r + " - " + r2 + " = "
						+ (r - r2));
				pn.Eval();
			}
		}
	}

	public interface Op {
		int Ex(double a, double b);
	}

	public void SetVariable(String key, double value) {
		varMap.put(key, new Double(value));
	}

	public void SetVariable(String key, Double value) {
		varMap.put(key, value);
	}

	// private String[] operators = {"|","^","&",">>","<<","+","-","*","/","%"};
	private String[][] operators = { { "|" }, { "^" }, { "&" }, { ">>", "<<" },
			{ "+", "-" }, { "*", "/", "%" } };

	public void InitializeOperators() {
		opMap.put("|", new Op() {
			public int Ex(double a, double b) {
				return (int) a | (int) b;
			}
		});
		opMap.put("^", new Op() {
			public int Ex(double a, double b) {
				return (int) a ^ (int) b;
			}
		});
		opMap.put("&", new Op() {
			public int Ex(double a, double b) {
				return (int) a & (int) b;
			}
		});
		opMap.put("%", new Op() {
			public int Ex(double a, double b) {
				return (int) (a % b);
			}
		});
		opMap.put("+", new Op() {
			public int Ex(double a, double b) {
				return (int) (a + b);
			}
		});
		opMap.put("-", new Op() {
			public int Ex(double a, double b) {
				return (int) (a - b);
			}
		});
		opMap.put("*", new Op() {
			public int Ex(double a, double b) {
				return (int) a * (int) b;
			}
		});
		opMap.put("/", new Op() {
			public int Ex(double a, double b) {
				return (int) (a / b);
			}
		});
		opMap.put(">>", new Op() {
			public int Ex(double a, double b) {
				return (int) a >> (int) b;
			}
		});
		opMap.put("<<", new Op() {
			public int Ex(double a, double b) {
				return (int) a << (int) b;
			}
		});
	}

	public FastParse() {
		InitializeOperators();
	}

	public String ob = "(";
	public String cb = ")";

	Pattern num = Pattern.compile("[0-9()]+");
	Pattern hex = Pattern.compile("\\b0[xX][0-9a-fA-F]+\\b");
	Pattern var = Pattern.compile("[a-zA-Z0-9()]+");

	public enum parseType {
		Expression, Value, Variable
	};

	public parseType DetermineParseType(String s) {
		Matcher numMatcher = num.matcher(s);
		Matcher hexMatcher = hex.matcher(s);
		Matcher varMatcher = var.matcher(s);
		if (numMatcher.matches() || hexMatcher.matches())
			return parseType.Value;
		if (varMatcher.matches())
			return parseType.Variable;
		return parseType.Expression;
	}

	public String RemoveRedundantBrackets(String s) {
		for (int i = 0; i < s.length(); ++i) {
			String c = s.substring(i, i + 1);
			if (c.compareTo(ob) == 0) {
				int closing = FindClosingBracket(s, i);
				if (i == 0 && closing == s.length() - 1) {
					s = s.substring(i + 1, closing);
					i = -1;
				}
			}
		}
		return s;
	}

	public int FindClosingBracket(String s, int index) {
		int depth = 0;
		for (int i = index; i < s.length(); ++i) {
			String c = s.substring(i, i + 1);
			if (c.compareTo(ob) == 0)
				++depth;
			else if (c.compareTo(cb) == 0)
				--depth;
			if (depth == 0) {
				return i;
			}
		}
		return -1;
	}

	public int[] FindNextOperator(String s) {
		for (String[] ops : operators) {
			for (int i =  s.length() - 1; i >= 0 ; --i) {
				for (String op : ops) {
					int opSize = op.length();
					String c = s.substring((i - opSize <= 0) ? i
							: i - opSize, i);
					if (c.contains(ob)) {
						i = FindClosingBracket(s, i);
					}
					if (i < 0)
						return new int[] {};

					boolean found = c.compareTo(op) == 0;
					if (found) {
						return new int[] { i, i + opSize };
					}
				}
			}
		}
		return null;
	}

	public String[] SplitExpression(String s, int begin, int end) {
		String s0 = s.substring(0, begin);
		String s1 = s.substring(begin, end);
		String s2 = s.substring(end);
		return new String[] { s0, s1, s2 };
	}

	public ParseNode Parse(String s) {
		ParseNode p0 = null, p1 = null;

		s = s.replace(" ", "");
		String[] subExp = null;

		s = RemoveRedundantBrackets(s);
		parseType t = DetermineParseType(s);
		switch (t) {
		case Expression:
			int[] result = FindNextOperator(s);
			if (result != null) {
				subExp = SplitExpression(s, result[0], result[1]);
				p0 = Parse(subExp[0]);
				p1 = Parse(subExp[2]);
				return (p0 != null && p1 != null) ? new ParseNode(
						opMap.get(subExp[1]), p0, p1) : 
							null;
			}
			break;
		case Value:
			int value = parseValue(s);
			return new ParseNode(value);
		case Variable:
			return new ParseNode(s);
		default:
			break;
		}

		return null;
	}

	public int parseValue(String s) {
		if (hex.matcher(s).matches()) {
			s = s.replaceAll("\\b0[xX]", "");
			return Integer.parseInt(s, 16);
		} else
			return Integer.parseInt(s);
	}

	public class ParseNode {
		private Op o;
		private ParseNode p0;
		private ParseNode p1;
		private int intValue;
		private String variableKey = null;
		private parseType ptype = null;

		public ParseNode(Op o, ParseNode p0, ParseNode p1) {
			this.o = o;
			this.p0 = p0;
			this.p1 = p1;
			ptype = parseType.Expression;
		}

		public ParseNode(int value) {
			intValue = value;
			ptype = parseType.Value;
		}

		public ParseNode(String s) {
			variableKey = s;
			ptype = parseType.Variable;
		}

		public double Eval() {
			switch (ptype) {
			case Expression:
				return o.Ex(p0.Eval(), p1.Eval());
			case Value:
				return intValue;
			case Variable:
				return varMap.get(variableKey).doubleValue();
			default:
				break;
			}
			return 0;
		}
	}
}

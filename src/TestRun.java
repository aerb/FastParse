
public class TestRun {
	public static void main(String[] args) {
		FastParse fp = new FastParse();
		fp.tryParse("1 + 1");
		System.out.println(fp.evaluate());
	}
}

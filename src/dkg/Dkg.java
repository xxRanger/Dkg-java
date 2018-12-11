package dkg;
import java.util.List;
import java.math.BigInteger;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.stream.Collectors;

class DkgFactory implements Supplier<Dkg> {
	
	private final int g;
	private final int t;
	private final int n;
	private final BigInteger p;
	private final int lowerBound;
	private final int upperBound;
	public Function<BigInteger, BigInteger> func;
	private static final Random random = new Random();
	
	public DkgFactory(int g,int t,int n, BigInteger p, int lowerBound, int upperBound) {
		this.g = g;
		this.t = t;
		this.n = t;
		this.p = p;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}
	
	private List<Integer> genParas() {
		return random.ints(n,lowerBound,upperBound)
			  .parallel()
			  .boxed()
			  .collect(Collectors.toList());
	}
	
	public Dkg get() {
		List<Integer> paras = genParas();
		return new Dkg(paras,g,t,n,p);
	}
}

public class Dkg {
	private final int g;
	private final int t;
	private final int n;
	private final BigInteger p;
	private final List<Integer> paras;
	public final List<BigInteger> shares;
	public final List<BigInteger> publicVals;
	private List<BigInteger> finalShares;
	private BigInteger finalSecret;
	public Function<BigInteger, BigInteger> finalFunc;
	
	public Dkg(List<Integer> paras, int g, int t, int n, BigInteger p) {
		this.t = t;
		this.p = p;
		this.g = g;
		this.n = n;
		this.paras = paras;
		this.shares = computeShares();
		this.publicVals = computePublicVals();
	}
	
	public synchronized void setFinalShares(List<BigInteger> finalShares) {
		this.finalShares = List.copyOf(finalShares);
	}
	
	public synchronized void setFinalSecret(BigInteger finalSecret) {
		this.finalSecret = new BigInteger(finalSecret.toByteArray());
	}
	
	public synchronized Object[] getFinalShares() {
		return finalShares.toArray();
	}
	
	public synchronized BigInteger getFinalSecret() {
		return new BigInteger(finalSecret.toByteArray());
	}
	
	private Function<BigInteger,BigInteger> bindPowMod (final BigInteger base) {
		return (i) -> base.modPow(i,p);
	}
	
	private Function<Integer,BigInteger> bindPow (final BigInteger base) {
		return (i) -> base.pow(i);
	}
	
	public BigInteger f(final BigInteger z) {
		Function<BigInteger,BigInteger> zPow = bindPowMod(z);
		return IntStream.range(0,paras.size())
				 .boxed()
				 .parallel()
				 .map(i-> BigInteger.valueOf(paras.get(i)).multiply(zPow.apply(BigInteger.valueOf(i))))
				 .reduce((a,b)-> a.add(b))
				 .get();
	}
	
	private List<BigInteger> computeShares() {
		return IntStream.rangeClosed(1,n)
				 .boxed()
				 .parallel()
				 .map(i->f(BigInteger.valueOf(i)))
				 .collect(Collectors.toList());
	}
	
	private List<BigInteger> computePublicVals() {
		Function<Integer,BigInteger> gPow = bindPow(BigInteger.valueOf(g));
		
		return IntStream.range(0,t)
				 .boxed()
				 .parallel()
				 .map(i-> gPow.apply(paras.get(i).intValue()))
				 .collect(Collectors.toList());
	}
	
	public boolean verifyPublicVals(int j, BigInteger shareJ, List<BigInteger> publicVals) {
		// check range
		if(j>n) {
			System.out.println("target out of range");
			return false;
		}
		if(publicVals.size()!=t) {
			System.out.println("public vals is less than t");
			return false;
		}
		
		BigInteger gShare = bindPowMod(BigInteger.valueOf(g))
										.apply(shareJ);
		
		Function<Integer,BigInteger> targetPow = bindPow(BigInteger.valueOf(j));
		BigInteger combindedVals = IntStream.range(0,n)
									 		 .boxed()
									 		 .parallel()
									 		 .map(i->bindPowMod(publicVals.get(i)).apply(targetPow.apply(i)))
									 		 .reduce((a,b)->a.multiply(b))
									 		 .get();
		
		return gShare.equals(combindedVals);
	}
	
	public static Supplier<Dkg> getSupplier(int g, int t, int n, BigInteger p,int lowerBound, int upperBound) {
		return new DkgFactory(g,t,n,p,lowerBound,upperBound);
	}
}

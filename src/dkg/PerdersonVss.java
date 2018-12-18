package dkg;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


class PerdersonVssFactory implements Supplier<PerdersonVss> {
	
	private final int g;
	private final int t;
	private final int n;
	private final int h;
	private final BigInteger p;
	private final int lowerBound;
	private final int upperBound;
	public Function<BigInteger, BigInteger> func;
	private static final Random random = new Random();
	
	public PerdersonVssFactory(int g, int h,int t,int n, BigInteger p, int lowerBound, int upperBound) {
		this.g = g;
		this.h = h;
		this.t = t;
		this.n = n;
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
	
	public PerdersonVss get() {
		List<Integer> paras1 = genParas();
		List<Integer> paras2 = genParas();
		return new PerdersonVss(paras1,paras2,g,h,t,n,p);
	}
}

public class PerdersonVss {
	private final int g;
	private final int h;
	private final int t;
	private final int n;
	private final BigInteger p;
	private BigInteger finalSecret1;
	private BigInteger finalSecret2;
	private BigInteger finalPublicVal;
	public final Function<BigInteger,BigInteger>f1;
	public final Function<BigInteger,BigInteger>f2;
	public final List<BigInteger> shares1;
	public final List<BigInteger> shares2;
	public final List<BigInteger> publicVals1;
	public final List<BigInteger> publicVals2;
	public final List<BigInteger> publicVals;
	
	public PerdersonVss(List<Integer> paras1,List<Integer> paras2, int g, int h,int t, int n, BigInteger p) {
		this.t = t;
		this.p = p;
		this.g = g;
		this.h = h;
		this.n = n;
		this.f1 = (BigInteger z)-> func(paras1, z);
		this.f2 = (BigInteger z)-> func(paras2, z);
		this.shares1 = computeShares(f1);
		this.shares2 = computeShares(f2);
		
		this.publicVals1 = computePublicVals(paras1,g);
		this.publicVals2 = computePublicVals(paras2,h);
		this.publicVals = combineTwoPublicVals();
	}
	
	public synchronized void setFinalSecret1(BigInteger finalSecret) {
		this.finalSecret1 = new BigInteger(finalSecret.toByteArray());
	}
	
	public synchronized void setFinalSecret2(BigInteger finalSecret) {
		this.finalSecret2 = new BigInteger(finalSecret.toByteArray());
	}
	
	public synchronized void setFinalPublicVal(BigInteger finalPublicVal) {
		this.finalPublicVal = finalPublicVal;
	}
	
	public synchronized BigInteger getFinalSecret1() {
		return new BigInteger(finalSecret1.toByteArray());
	}
	
	public synchronized BigInteger getFinalSecret2() {
		return new BigInteger(finalSecret2.toByteArray());
	}
	
	public synchronized BigInteger finalPublicVal() {
		return new BigInteger(finalPublicVal.toByteArray());
	}
	
	private Function<BigInteger,BigInteger> bindPowMod (final BigInteger base) {
		return (i) -> base.modPow(i,p);
	}
	
	private Function<Integer,BigInteger> bindPow (final BigInteger base) {
		return (i) -> base.pow(i);
	}
	
	private BigInteger func(List<Integer> paras ,final BigInteger z) {
		Function<BigInteger,BigInteger> zPow = bindPowMod(z);
		return IntStream.range(0,paras.size())
				 .boxed()
				 .parallel()
				 .map(i-> BigInteger.valueOf(paras.get(i)).multiply(zPow.apply(BigInteger.valueOf(i))))
				 .reduce((a,b)-> a.add(b))
				 .get();
	}
	
	private List<BigInteger> computeShares(Function<BigInteger,BigInteger>f) {
		return IntStream.rangeClosed(1,n)
				 .boxed()
				 .parallel()
				 .map(i->f.apply(BigInteger.valueOf(i)))
				 .collect(Collectors.toList());
	}
	
	private List<BigInteger> computePublicVals(List<Integer> paras, int generatorBase) {
		Function<Integer,BigInteger> gPow = bindPow(BigInteger.valueOf(generatorBase));
		
		return IntStream.range(0,t)
				 .boxed()
				 .parallel()
				 .map(i-> gPow.apply(paras.get(i).intValue()))
				 .collect(Collectors.toList());
	}
	
	private List<BigInteger> combineTwoPublicVals() {
		return IntStream.range(0,t)
					   .boxed()
					   .parallel()
					   .map(i-> publicVals1.get(i).multiply(publicVals2.get(i)).mod(p))
					   .collect(Collectors.toList());
	}
	
	public boolean verifyPublicValsFirstStage(int j, BigInteger shareJ1, BigInteger ShareJ2, List<BigInteger> publicVals) {
		BigInteger gShare = bindPowMod(BigInteger.valueOf(g)).apply(shareJ1);
		BigInteger hShare = bindPowMod(BigInteger.valueOf(h)).apply(ShareJ2);
		BigInteger share = gShare.multiply(hShare).mod(p);
		return verifyPublicVals(j, share, publicVals);
	}
	
	public boolean verifyPublicValsFinalStage(int j, BigInteger shareJ, List<BigInteger> publicVals) {
		BigInteger gShare = bindPowMod(BigInteger.valueOf(g))
				.apply(shareJ);
		
		return verifyPublicVals(j, gShare,  publicVals);
	}
	
	private boolean verifyPublicVals(int j, BigInteger share, List<BigInteger> publicVals) {
		// check range
		if(j>n) {
			System.out.println("target out of range");
			return false;
		}
		if(publicVals.size()!=t) {
			System.out.println("public vals is less than t");
			return false;
		}
		
		Function<Integer,BigInteger> targetPow = bindPow(BigInteger.valueOf(j));
		
		BigInteger combindedVals = IntStream.range(0,t)
									 		 .boxed()
									 		 .parallel()
									 		 .map(i->bindPowMod(publicVals.get(i)).apply(targetPow.apply(i)))
									 		 .reduce((a,b)->a.multiply(b))
									 		 .get();
		
		return share.equals(combindedVals);
	}
	
	public List<Integer> pickQual(List<PerdersonVss> subDkgs, int hostIndex) {
		return IntStream.range(0,subDkgs.size())
		 .boxed()
		 .parallel()
		 .filter(j-> ! verifyPublicValsFirstStage(j,
						 subDkgs.get(j).shares1.get(hostIndex), 
						 subDkgs.get(j).shares2.get(hostIndex), 
						 subDkgs.get(j).publicVals))
		 .limit(t)
		 .collect(Collectors.toList());
	}
	
	public static Supplier<PerdersonVss> getSupplier(int g, int h, int t, int n, BigInteger p,int lowerBound, int upperBound) {
		return new PerdersonVssFactory(g,h,t,n,p,lowerBound,upperBound);
	}
	
	public BigInteger genFinalSecret1(List<Integer> qual,List<PerdersonVss> subDkgs, int hostIndex) {
		return qual.parallelStream()
		 .map(i->subDkgs.get(i).shares1.get(hostIndex))
		 .reduce((a,b)-> a.add(b))
		 .get();
	}
	
	public BigInteger genFinalSecret2(List<Integer> qual,List<PerdersonVss> subDkgs, int hostIndex) {
		return qual.parallelStream()
		 .map(i->subDkgs.get(i).shares2.get(hostIndex))
		 .reduce((a,b)-> a.add(b))
		 .get();
	}
	
	public BigInteger genFinalPublicVal(List<Integer> qual,List<PerdersonVss> subDkgs) {
		return qual.parallelStream()
		.map(i->subDkgs.get(i).publicVals1.get(0))
		.reduce((a,b)-> a.multiply(b).mod(p))
		.get();
	}
	
	public List<Integer> genToConstruct(List<Integer> qual,List<PerdersonVss> subDkgs,int hostIndex) {
		return qual.parallelStream()
		 .filter(j-> verifyPublicValsFinalStage(j,
						 subDkgs.get(j).shares1.get(hostIndex), 
						 subDkgs.get(j).publicVals))
		 .collect(Collectors.toList());
	}
}

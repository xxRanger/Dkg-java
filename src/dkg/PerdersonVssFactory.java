package dkg;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerdersonVssFactory implements Supplier<PerdersonVss> {
	
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
	
	private Function<BigInteger,BigInteger> bindPowMod (final BigInteger base) {
		return (i) -> base.modPow(i,p);
	}
	
	private Function<Integer,BigInteger> bindPow (final BigInteger base) {
		return (i) -> base.pow(i);
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
	
	public List<Integer> calSingleQual(List<PerdersonVss> dkgPoints, int hostIndex) {
		return IntStream.range(0,dkgPoints.size())
		 .boxed()
		 .parallel()
		 .filter(j-> ! verifyPublicValsFirstStage(j,
				 dkgPoints.get(j).shares1.get(hostIndex), 
				 dkgPoints.get(j).shares2.get(hostIndex), 
				 dkgPoints.get(j).publicVals))
		 .collect(Collectors.toList());
	}
	
	public List<List<Integer>> calCollectedQuals(List<PerdersonVss> dkgPoints) {
		return IntStream.range(0,dkgPoints.size())
		.boxed()
		.parallel()
		.map(hostIndex-> calSingleQual(dkgPoints, hostIndex))
		.collect(Collectors.toList());
	}
	
	public List<Integer> genToConstruct(List<Integer> qual,List<PerdersonVss> dkgPoints,int hostIndex) {
		return qual.parallelStream()
		 .filter(j-> verifyPublicValsFinalStage(j,
						 dkgPoints.get(j).shares1.get(hostIndex), 
						 dkgPoints.get(j).publicVals))
		 .collect(Collectors.toList());
	}
	
	public BigInteger calSecret1(List<Integer> qual,List<PerdersonVss> dkgPoints, int hostIndex) {
		return calFinalVal(qual, i-> dkgPoints.get(i).shares1.get(hostIndex));
	}
	
	public BigInteger calSecret2(List<Integer> qual,List<PerdersonVss> dkgPoints, int hostIndex) {
		return calFinalVal(qual, i-> dkgPoints.get(i).shares2.get(hostIndex));
	}
	
	public BigInteger calPublicVal(List<Integer> qual,List<PerdersonVss> dkgPoints) {
		return calFinalVal(qual, i-> dkgPoints.get(i).publicVals1.get(0));
	}
	
	public List<BigInteger> calCollectedSecret1(List<List<Integer>> quals, List<PerdersonVss> dkgPoints) {
		return calCollectedFinalVal(quals, dkgPoints, (i,j)-> dkgPoints.get(i).shares1.get(j));
	}
	
	public List<BigInteger> calCollectedSecret2(List<List<Integer>> quals, List<PerdersonVss> dkgPoints) {
		return calCollectedFinalVal(quals, dkgPoints, (i,j)-> dkgPoints.get(i).shares2.get(j));
	}
	
	public List<BigInteger> calCollectedPublicVal(List<List<Integer> > quals, List<PerdersonVss> dkgPoints) {
		return calCollectedFinalVal(quals, dkgPoints, (i,j)-> dkgPoints.get(i).publicVals1.get(0));
	}
	
	private List<BigInteger> calCollectedFinalVal(List<List<Integer> > quals, List<PerdersonVss> dkgPoints, BiFunction<Integer, Integer, BigInteger> valSupplier ) {
		return IntStream.range(0,dkgPoints.size())
				.boxed()
				.parallel()
				.map(hostIndex-> calFinalVal(quals.get(hostIndex), i->valSupplier.apply(i,hostIndex)))
				.collect(Collectors.toList());
	}
	
	private BigInteger calFinalVal(List<Integer> qual, Function<Integer, BigInteger> valSupplier) {
		return qual.parallelStream()
				.map(i-> valSupplier.apply(i))
				.reduce((a,b)-> a.multiply(b).mod(p))
				.get(); 
	}
}

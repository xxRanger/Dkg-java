package dkg;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import utils.Utils;

public class PerdersonVss {
	private final BigInteger g;
	private final BigInteger h;
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
	
	public PerdersonVss(List<Integer> paras1,List<Integer> paras2, BigInteger g, BigInteger h,int t, int n, BigInteger p) {
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
	
	private List<BigInteger> computePublicVals(List<Integer> paras, BigInteger generatorBase) {
		Function<Integer,BigInteger> gPow = bindPow(generatorBase);
		
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

}

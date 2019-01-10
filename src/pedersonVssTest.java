import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dkg.PerdersonVss;

public class PedersonVssTest {
	private static final int g = 2;
	private static final int h = 3;
	private static final BigInteger p = new BigInteger("157754757658850164039820501368692494984638811981595753785726084071390339342949827166074468203116945260071420591948184266427919389750857419939387549499186051557325946160152109714671771886387784860670680481921786590260608186162263954672484772147274284399498187140357851764561666898851637006570752518678867635307");
	private static final int lowerBound = 10;
	private static final int upperBound = 10000;
	
	private <V> Pair<V,Long> executionTime(Callable<V>func){
	  long startTime = System.currentTimeMillis();
	  V r=null;
	  try {
		  r = func.call();
	  } catch (Exception e) {
		  System.out.println(e.getMessage());
	  }
      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      return new Pair<V,Long>(r,elapsedTime);
	}
	
	private class Pair<T,U> {
		public final T first;
		public final U second;
		Pair(T first, U second){
			this.first = first;
			this.second = second;
		}
	}
	
	private static void testTime(int t,int n) {
		PedersonVssTest test = new PedersonVssTest();
		Supplier<PerdersonVss> dkgSupplier = PerdersonVss.getSupplier(g, h, t, n, p, lowerBound, upperBound);
		
		
		System.out.println("time for initialize sub dkgs");
		Pair<List<PerdersonVss>, Long> rsubDkgs = test.executionTime(()-> Stream.generate(dkgSupplier)
				  .limit(n-1)
				  .collect(Collectors.toList()));
		List<PerdersonVss> subDkgs = rsubDkgs.first;
		long genSubDkgs = rsubDkgs.second;
		final int hostIndex = 0;
		System.out.println("exceution time:"+genSubDkgs+"ms");
		
		
		System.out.println("time for initialize single dkg");
		
		Pair<PerdersonVss, Long> rdkg = test.executionTime(()->dkgSupplier.get());
		PerdersonVss hostDkg = rdkg.first;
		long genDkgTime = rdkg.second;
		System.out.println("exceution time:"+genDkgTime+"ms");
		
		
		System.out.println("time for calculate qualified subDkgs");
		Pair<List<Integer>, Long> rQual = test.executionTime(()->hostDkg.pickQual(subDkgs, hostIndex));
		List<Integer> qual= rQual.first;
		long genQualTime = rQual.second;
		System.out.println("exceution time:"+genQualTime+"ms");
		
		
		System.out.println("time for generate final secret");
		Pair<BigInteger, Long> rSecret = test.executionTime(()->hostDkg.genFinalSecret1(qual, subDkgs, hostIndex));
		BigInteger finalSecret1 = rSecret.first;
		long genFinalSecret1Time = rSecret.second;
		rSecret = test.executionTime(()->hostDkg.genFinalSecret2(qual, subDkgs, hostIndex));
		BigInteger finalSecret2 = rSecret.first;
		long genFinalSecret2Time = rSecret.second;
		System.out.println("exceution time:"+(genFinalSecret1Time+genFinalSecret2Time)+"ms");
		
		
		System.out.println("time for generate final public val");
		Pair<BigInteger, Long> rPublicVal = test.executionTime(()->hostDkg.genFinalPublicVal(qual, subDkgs));
		BigInteger finalPublicVal = rPublicVal.first;
		long genFinalPublicValTime = rPublicVal.second;
		System.out.println("exceution time:"+genFinalPublicValTime+"ms");
		
		long totalTime = genDkgTime + genQualTime + genFinalSecret1Time + genFinalSecret2Time + genFinalPublicValTime;
		System.out.println("total time:"+totalTime);
	}
	

	public static void main(String[] args) {
		
		// ---------------------------------------------------------------
		// 1.initialize parameter
		// ---------------------------------------------------------------
		
		// t, n from terminal
		if(args.length < 2) {
			System.out.println("please input n and t");
			System.exit(-1);
		}
		
		int t = Integer.parseInt(args[0]);
		int n = Integer.parseInt(args[1]);
		
		//examine input
		if(t<0 || n<0 ) {
			System.out.println("input should be positive");
			System.exit(-1);
		}
		
		if(t>n) {
			System.out.println("t should be smaller than n");
			System.exit(-1);
		}
		
		// for testTime
//		t = 10;
//		n = 30;
//		System.out.println("t: "+t+" n: "+n);
//		testTime(t, n);
		//
		
		// ---------------------------------------------------------------
		// 2.Broadcast DKG and receive DKG
		// ---------------------------------------------------------------
		
		Supplier<PerdersonVss> dkgSupplier = PerdersonVss.getSupplier(g, h, t, n, p, lowerBound, upperBound);
		
		// host dkg, receive n-1 dkgs from sub network
		PerdersonVss hostDkg = dkgSupplier.get();
		final int hostIndex = 0;
		
		
		// broadcast host dkg
		// 
		
		
		
		// receive from sub dkgs
		List<PerdersonVss> subDkgs = Stream.generate(dkgSupplier)
								  .limit(n-1)
								  .collect(Collectors.toList());
			  
		// calculate qualfied dkg
		List<Integer> qual = hostDkg.pickQual(subDkgs, hostIndex);
		
		if(qual.isEmpty()) {
			System.out.println("no valid dkg");
			return;
		}
		
		// waiting for complaints
		// verifyResponseFromComplaintee();
		// if qual less than t ; then addVeriedComplainerToQual()
		
		System.out.println("total number:" + n);
		System.out.println("valid number: " + qual.size());
		
		// ---------------------------------------------------------------
		// 3.start to generate result
		// ---------------------------------------------------------------
		
		
		
		// ---------------------------------------------------------------
		// 3.1 calculate final secret
		// ---------------------------------------------------------------
				
		
		BigInteger finalSecret1 = hostDkg.genFinalSecret1(qual, subDkgs, hostIndex);
		
		BigInteger finalSecret2 = hostDkg.genFinalSecret2(qual, subDkgs, hostIndex);
		
		// ---------------------------------------------------------------
		// 3.2 Broadcast and calculate final publicVals
		// ---------------------------------------------------------------
		
		
		// broadcast yi (shares1)
		// broadcast()
		
		// receive from broadcast, verify
		List<Integer> toConstruct = hostDkg.genToConstruct(qual, subDkgs, hostIndex);
		
		System.out.println("to be constructed: "+toConstruct);
		
		// construct for subhost in toConstruct
		// construct() TO BE DONE
		
		// calculate final publicVal
		
		BigInteger finalPublicVal = hostDkg.genFinalPublicVal(qual, subDkgs);
		
		
		
		// ---------------------------------------------------------------
		// 4. Set final states
		// ---------------------------------------------------------------
		
		hostDkg.setFinalSecret1(finalSecret1);
		hostDkg.setFinalSecret1(finalSecret2);
		hostDkg.setFinalPublicVal(finalPublicVal);
		
		System.out.println("final secret1: "+ finalSecret1);
		System.out.println("final secret2: "+ finalSecret2);
		System.out.println("final public val: "+ finalPublicVal);
	}	
}

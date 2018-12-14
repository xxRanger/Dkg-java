import java.math.BigInteger;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dkg.PerdersonVss;;

public class pedersonVssTest {
	private static final int g = 42;
	private static final int h = 37;
	private static final BigInteger p = BigInteger.valueOf(65537);
	private static final int lowerBound = 10;
	private static final int upperBound = 10000;

	public static void main(String[] args) {
		
		// ---------------------------------------------------------------
		// 1.initialize parameter
		// ---------------------------------------------------------------
		
		// t, n from terminal
		if(args.length < 2) {
			System.out.println("please input n and t");
			System.exit(-1);
		}
		
		final int t = Integer.parseInt(args[0]);
		final int n = Integer.parseInt(args[1]);
		
		
		//examine input
		if(t<0 || n<0 ) {
			System.out.println("input should be positive");
			System.exit(-1);
		}
		
		if(t>n) {
			System.out.println("t should be smaller than n");
			System.exit(-1);
		}
		
		
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
		List<Integer> qual = IntStream.range(0,subDkgs.size())
						 .boxed()
						 .parallel()
						 .filter(j-> !hostDkg.verifyPublicValsFirstStage(j,
										 subDkgs.get(j).shares1.get(hostIndex), 
										 subDkgs.get(j).shares2.get(hostIndex), 
										 subDkgs.get(j).publicVals))
						 .limit(t)
						 .collect(Collectors.toList());
		
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
				
		
		BigInteger finalSecret1 = qual.parallelStream()
									 .map(i->subDkgs.get(i).shares1.get(hostIndex))
									 .reduce((a,b)-> a.add(b))
									 .get();
		
		BigInteger finalSecret2 = qual.parallelStream()
									 .map(i->subDkgs.get(i).shares2.get(hostIndex))
									 .reduce((a,b)-> a.add(b))
									 .get();
		
		// ---------------------------------------------------------------
		// 3.2 Broadcast and calculate final publicVals
		// ---------------------------------------------------------------
		
		
		// broadcast yi (shares1)
		// broadcast()
		
		// receive from broadcast, verify
		List<Integer> toConstruct = qual.parallelStream()
				 .filter(j-> hostDkg.verifyPublicValsFinalStage(j,
								 subDkgs.get(j).shares1.get(hostIndex), 
								 subDkgs.get(j).publicVals))
				 .collect(Collectors.toList());
		
		System.out.println("to be constructed: "+toConstruct);
		
		// construct for subhost in toConstruct
		// construct() TO BE DONE
		
		// calculate final publicVal
		
		BigInteger finalPublicVal = qual.parallelStream()
										.map(i->subDkgs.get(i).publicVals1.get(0))
										.reduce((a,b)-> a.multiply(b).mod(p))
										.get();
		
		
		
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

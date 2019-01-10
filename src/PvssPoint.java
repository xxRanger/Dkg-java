import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dkg.PerdersonVss;
import dkg.PerdersonVssFactory;

public class PvssPoint {
	private static final int g = 2;
	private static final int h = 3;
	private static final BigInteger p = new BigInteger("157754757658850164039820501368692494984638811981595753785726084071390339342949827166074468203116945260071420591948184266427919389750857419939387549499186051557325946160152109714671771886387784860670680481921786590260608186162263954672484772147274284399498187140357851764561666898851637006570752518678867635307");
	private static final int lowerBound = 10;
	private static final int upperBound = 10000;
	
	public static void main(String[] args) {

				int t = 5;
				int n = 10;
				
				PerdersonVssFactory dkgSupplier = new PerdersonVssFactory(g, h, t, n, p, lowerBound, upperBound);
				List<PerdersonVss> dkgPoints = Stream.generate(dkgSupplier)
						.limit(n)
						.collect(Collectors.toList());
				
				List< List<Integer> > quals =  dkgSupplier.calCollectedQuals(dkgPoints);
				List<BigInteger>  secret1s = dkgSupplier.calCollectedSecret1(quals, dkgPoints);
				List<BigInteger> secret2s = dkgSupplier.calCollectedSecret2(quals, dkgPoints);
				List<BigInteger> publicVals = dkgSupplier.calCollectedPublicVal(quals, dkgPoints);
				for(BigInteger a: publicVals) {
					System.out.println(a);
				}
				
				// for testTime
//				t = 10;
//				n = 30;
//				System.out.println("t: "+t+" n: "+n);
//				testTime(t, n);
				//
				
				// ---------------------------------------------------------------
				// 2.Broadcast DKG and receive DKG
				// ---------------------------------------------------------------
				
//				Supplier<PerdersonVss> dkgSupplier = PerdersonVss.getSupplier(g, h, t, n, p, lowerBound, upperBound);
//				
//				// host dkg, receive n-1 dkgs from sub network
//				PerdersonVss hostDkg = dkgSupplier.get();
//				final int hostIndex = 0;
//				
//				
//				// broadcast host dkg
//				// 
//				
//				
//				
//				// receive from sub dkgs
//				List<PerdersonVss> subDkgs = Stream.generate(dkgSupplier)
//										  .limit(n-1)
//										  .collect(Collectors.toList());
//					  
//				// calculate qualfied dkg
//				List<Integer> qual = hostDkg.pickQual(subDkgs, hostIndex);
//				
//				if(qual.isEmpty()) {
//					System.out.println("no valid dkg");
//					return;
//				}
//				
//				// waiting for complaints
//				// verifyResponseFromComplaintee();
//				// if qual less than t ; then addVeriedComplainerToQual()
//				
//				System.out.println("total number:" + n);
//				System.out.println("valid number: " + qual.size());
//				
//				// ---------------------------------------------------------------
//				// 3.start to generate result
//				// ---------------------------------------------------------------
//				
//				
//				
//				// ---------------------------------------------------------------
//				// 3.1 calculate final secret
//				// ---------------------------------------------------------------
//						
//				
//				BigInteger finalSecret1 = hostDkg.genFinalSecret1(qual, subDkgs, hostIndex);
//				
//				BigInteger finalSecret2 = hostDkg.genFinalSecret2(qual, subDkgs, hostIndex);
//				
//				// ---------------------------------------------------------------
//				// 3.2 Broadcast and calculate final publicVals
//				// ---------------------------------------------------------------
//				
//				
//				// broadcast yi (shares1)
//				// broadcast()
//				
//				// receive from broadcast, verify
//				List<Integer> toConstruct = hostDkg.genToConstruct(qual, subDkgs, hostIndex);
//				
//				System.out.println("to be constructed: "+toConstruct);
//				
//				// construct for subhost in toConstruct
//				// construct() TO BE DONE
//				
//				// calculate final publicVal
//				
//				BigInteger finalPublicVal = hostDkg.genFinalPublicVal(qual, subDkgs);
//				
//				
//				
//				// ---------------------------------------------------------------
//				// 4. Set final states
//				// ---------------------------------------------------------------
//				
//				hostDkg.setFinalSecret1(finalSecret1);
//				hostDkg.setFinalSecret1(finalSecret2);
//				hostDkg.setFinalPublicVal(finalPublicVal);
//				
//				System.out.println("final secret1: "+ finalSecret1);
//				System.out.println("final secret2: "+ finalSecret2);
//				System.out.println("final public val: "+ finalPublicVal);
	}
}
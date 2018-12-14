import dkg.JointFeldman;
import java.util.function.Supplier;
import java.util.List;
import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class JointFeldmanTest {
	private static int g = 42;
	private static BigInteger p = BigInteger.valueOf(65537);
	private static int lowerBound = 10;
	private static int upperBound = 10000;

	public static void main(String[] args) {
		
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
		
		Supplier<JointFeldman> dkgSupplier = JointFeldman.getSupplier(g, t, n, p, lowerBound, upperBound);
		
		// host dkg, receive n-1 dkgs from sub network
		JointFeldman hostDkg = dkgSupplier.get();
		final int hostIndex = 0;
		
		List<JointFeldman> subDkgs = Stream.generate(dkgSupplier)
								  .limit(n-1)
								  .collect(Collectors.toList());
			  
		// calculate qualfied dkg
		List<Integer> qual = IntStream.range(0,subDkgs.size())
						 .boxed()
						 .parallel()
						 .filter(j-> !hostDkg.verifyPublicVals(j, subDkgs.get(j).shares.get(hostIndex), subDkgs.get(j).publicVals))
						 .limit(t)
						 .collect(Collectors.toList());
		
		if(qual.isEmpty()) {
			System.out.println("no valid dkg");
			return;
		}
		
		System.out.println("total number:" + n);
		System.out.println("valid number: " + qual.size());
		//calculate final shares
		List<BigInteger> finalShares = IntStream.range(0,qual.size())
				 .parallel()
				 .boxed()
				 .map( i-> qual.parallelStream()
							   .map(j-> subDkgs.get(j).publicVals.get(i))
							   .reduce( (a,b) -> a.multiply(b).mod(p))
							   .get())
				 .collect(Collectors.toList());
		
	
		
		//calculate final secrets and set final secrets to host 
		
		BigInteger finalSecret = qual.parallelStream()
									 .map(i->subDkgs.get(i).shares.get(hostIndex))
									 .reduce((a,b)-> a.add(b))
									 .get();
		
		// calculate new f
		BigInteger finalPublicVal = qual.parallelStream()
										.map(i->subDkgs.get(i).publicVals.get(0))
										.reduce((a,b)-> a.multiply(b).mod(p))
										.get();
		
		//set final shares , secrets, final func to host dkg
		hostDkg.setFinalShares(finalShares);
		hostDkg.setFinalSecret(finalSecret);
		hostDkg.setFinalPublicVal(finalPublicVal);
		
		System.out.println("final shares: "+ finalShares);
		System.out.println("final secret: "+finalSecret);
		System.out.println("final public val: "+ finalPublicVal);
	}	
}

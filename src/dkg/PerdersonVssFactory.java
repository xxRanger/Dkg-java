package dkg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class PerdersonVssFactory implements Supplier<PerdersonVss> {
	
	private final BigInteger g;
	private final int t;
	private final int n;
	private final BigInteger h;
	private final BigInteger p;
	private final int lowerBound;
	private final int upperBound;
	public Function<BigInteger, BigInteger> func;
	private static final Random random = new Random();
	
	public PerdersonVssFactory(BigInteger g, BigInteger h,int t,int n, BigInteger p, int lowerBound, int upperBound) {
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
		BigInteger gShare = bindPowMod(g).apply(shareJ1);
		BigInteger hShare = bindPowMod(g).apply(ShareJ2);
		BigInteger share = gShare.multiply(hShare).mod(p);
		return verifyPublicVals(j, share, publicVals);
	}
	
	public boolean verifyPublicValsFinalStage(int j, BigInteger shareJ, List<BigInteger> publicVals) {
		BigInteger gShare = bindPowMod(g)
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
		return calFinalVal(qual, i-> dkgPoints.get(i).shares1.get(hostIndex),(a,b)->a.add(b).mod(p));
	}
	
	public BigInteger calSecret2(List<Integer> qual,List<PerdersonVss> dkgPoints, int hostIndex) {
		return calFinalVal(qual, i-> dkgPoints.get(i).shares2.get(hostIndex),(a,b)->a.add(b).mod(p));
	}
	
	public BigInteger calPublicVal(List<Integer> qual,List<PerdersonVss> dkgPoints) {
		return calFinalVal(qual, i-> dkgPoints.get(i).publicVals1.get(0),(a,b)->a.multiply(b).mod(p));
	}
	
	public List<BigInteger> calCollectedSecret1(List<List<Integer>> quals, List<PerdersonVss> dkgPoints) {
		return calCollectedFinalVal(quals, dkgPoints, (i,j)-> dkgPoints.get(i).shares1.get(j),(a,b)->a.add(b).mod(p));
	}
	
	public List<BigInteger> calCollectedSecret2(List<List<Integer>> quals, List<PerdersonVss> dkgPoints) {
		return calCollectedFinalVal(quals, dkgPoints, (i,j)-> dkgPoints.get(i).shares2.get(j),(a,b)->a.add(b).mod(p));
	}
	
	public List<BigInteger> calCollectedPublicVal(List<List<Integer> > quals, List<PerdersonVss> dkgPoints) {
		return calCollectedFinalVal(quals, dkgPoints, (i,j)-> dkgPoints.get(i).publicVals1.get(0),(a,b)->a.multiply(b).mod(p));
	}
	
	private List<BigInteger> calCollectedFinalVal(List<List<Integer> > quals, List<PerdersonVss> dkgPoints, BiFunction<Integer, Integer, BigInteger> valSupplier,BiFunction<BigInteger,BigInteger,BigInteger> collector ) {
		return IntStream.range(0,dkgPoints.size())
				.boxed()
				.parallel()
				.map(hostIndex-> calFinalVal(quals.get(hostIndex), i->valSupplier.apply(i,hostIndex),collector))
				.collect(Collectors.toList());
	}
	
	private BigInteger calFinalVal(List<Integer> qual, Function<Integer, BigInteger> valSupplier, BiFunction<BigInteger,BigInteger,BigInteger> collector ) {
		return qual.parallelStream()
				.map(i-> valSupplier.apply(i))
				.reduce((a,b)->collector.apply(a,b))
				.get(); 
	}

	public byte[] SHA256(byte[]... data) {
		MessageDigest mDigest = null;
		try {
			mDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			System.out.println(e);
			System.exit(-1);
		}

		ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
		for(byte[]d:data) {
			try{
				dataStream.write(d);
			} catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}
		}

		return (new BigInteger(dataStream.toByteArray())).mod(p).toByteArray();
	}

	public List<BigInteger> encrypt(BigInteger m, BigInteger L, BigInteger publicVal) {
		System.out.println("to be encrpt: "+m);
		BigInteger _g = this.h;
		
		BigInteger h = publicVal;
		Function<BigInteger,BigInteger> hPow = bindPowMod(h);
		
		Random rand = new Random();
		BigInteger r = BigInteger.valueOf(rand.nextInt(Integer.MAX_VALUE)+1);
		BigInteger s = BigInteger.valueOf(rand.nextInt(Integer.MAX_VALUE)+1);
		
		// calculate c

		byte[] hPowR = SHA256(hPow.apply(r).toByteArray());

		BigInteger c = (new BigInteger(hPowR)).xor(m);
		
		// calculate u,w,_u,_w
		Function<BigInteger,BigInteger> gPow = bindPowMod(g);
		Function<BigInteger,BigInteger> _gPow = bindPowMod(_g);
		BigInteger u = gPow.apply(r);
		BigInteger w = gPow.apply(s);
		BigInteger _u = _gPow.apply(r);
		BigInteger _w = _gPow.apply(s);
		
		// calculate e
		BigInteger e = new BigInteger(SHA256(c.toByteArray()
				,L.toByteArray()
				,u.toByteArray()
				,w.toByteArray()
				,_u.toByteArray()
				,_w.toByteArray()));

		BigInteger f = s.add(r.multiply(e));
		
		return Arrays.asList(c,L,u,_u,e,f);
	}

	// Lagrange interpolation coefficients
	private BigInteger LILambda(List<Integer> qual, int i, int x) {
		BigInteger topHalf = qual.parallelStream()
				.filter(j-> j!=i)
				.map(j-> BigInteger.valueOf(x-(j+1)))
				.reduce((a,b)->a.multiply(b))
				.get();
		BigInteger bottomHalf = qual.parallelStream()
				.filter(j-> j!=i)
				.map(j-> BigInteger.valueOf(i-j))
				.reduce((a,b)->a.multiply(b))
				.get();
		return topHalf.divide(bottomHalf);
	}

	public BigInteger decrypt(List<BigInteger> ciphertext, List<PerdersonVss> dkgPoints, List<Integer> qual) {

		// ciphertext verification
		if(ciphertext.size()!=6) {
			System.out.println("ciphertext size not equal to 6");
			System.exit(-1);
		}

		BigInteger _g = this.h;
		BigInteger c = ciphertext.get(0);
		BigInteger L = ciphertext.get(1);
		BigInteger u = ciphertext.get(2);
		BigInteger _u = ciphertext.get(3);
		BigInteger e = ciphertext.get(4);
		BigInteger f = ciphertext.get(5);

		Function<BigInteger,BigInteger> gPow = bindPowMod(g);
		Function<BigInteger,BigInteger> uPow = bindPowMod(u);
		Function<BigInteger,BigInteger> _gPow = bindPowMod(_g);
		Function<BigInteger,BigInteger> _uPow = bindPowMod(_u);

		BigInteger w = gPow.apply(f).multiply(uPow.apply(e).modInverse(p)).mod(p);
		BigInteger _w = _gPow.apply(f).multiply(_uPow.apply(e).modInverse(p)).mod(p);

		BigInteger _e = new BigInteger(SHA256(
				c.toByteArray(),
				L.toByteArray(),
				u.toByteArray(),
				w.toByteArray(),
				_u.toByteArray(),
				_w.toByteArray()
		));

		if(!e.equals(_e)) {
			System.out.println("ciphertext is not right");
			System.out.println("current e: "+e);
			System.out.println("target _e: "+_e);
			System.exit(-1);
		}

		// calculate decryption shares

		List<List<BigInteger>> shares = IntStream.range(0,dkgPoints.size())
				.boxed()
				.parallel()
				.map((i)->{
					BigInteger xi = dkgPoints.get(i).getFinalSecret1();
					BigInteger ui = uPow.apply(xi);
					Random rand = new Random();
					BigInteger si = BigInteger.valueOf(rand.nextInt(Integer.MAX_VALUE)+1);
					BigInteger _ui = uPow.apply(si);
					BigInteger _hi = gPow.apply(si);
					BigInteger ei = new BigInteger(SHA256(ui.toByteArray(),
							_ui.toByteArray(),
							_hi.toByteArray()));
					BigInteger fi = si.add(xi.multiply(ei));
					return Arrays.asList(BigInteger.valueOf(i),ui,ei,fi); })
				.collect(Collectors.toList());

		// share verification
		shares.forEach(
				share-> {
					if(share.size()!=4) {
						System.out.println("share size not equal to 4");
						System.exit(-1);
					}

					BigInteger i  = share.get(0);
					BigInteger ui = share.get(1);
					Function<BigInteger,BigInteger> uiPow = bindPowMod(ui);
					BigInteger ei = share.get(2);
					BigInteger fi = share.get(3);
					BigInteger hi = gPow.apply(dkgPoints.get(i.intValue()).getFinalSecret1());
					Function<BigInteger,BigInteger> hiPow = bindPowMod(hi);
					BigInteger _ui = uPow.apply(fi).multiply(uiPow.apply(ei).modInverse(p)).mod(p);
					BigInteger _hi = gPow.apply(fi).multiply(hiPow.apply(ei).modInverse(p)).mod(p);

					BigInteger _ei = new BigInteger(SHA256(ui.toByteArray(),
							_ui.toByteArray(),
							_hi.toByteArray()
					));

					if(!ei.equals(_ei)) {
						System.out.println("share verification fail at "+i);
						System.out.println("current ei: "+ei);
						System.out.println("target _ei: "+_ei);
						System.exit(-1);
					}
				}
		);

		// recover message
		BigInteger m = qual.parallelStream()
				.map(i-> {
					BigInteger lambdai = LILambda(qual,i,0);
					BigInteger ui = shares.get(i).get(1);
					return ui.modPow(lambdai,p); })
				.reduce((a,b)-> a.multiply(b).mod(p))
				.get()
				.xor(c);
		System.out.println("decryption message: "+m);
		return m;
	}
}

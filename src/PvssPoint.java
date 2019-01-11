import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import java.security.MessageDigest;

import dkg.PerdersonVss;
import dkg.PerdersonVssFactory;

public class PvssPoint {
	private static final BigInteger g = new BigInteger("2");
	private static final BigInteger h = new BigInteger("3");
	private static final BigInteger p = new BigInteger("157754757658850164039820501368692494984638811981595753785726084071390339342949827166074468203116945260071420591948184266427919389750857419939387549499186051557325946160152109714671771886387784860670680481921786590260608186162263954672484772147274284399498187140357851764561666898851637006570752518678867635307");
	private static final int lowerBound = 10;
	private static final int upperBound = 10000;
	
	public static void main(String[] args) {

				int t = 5;
				int n = 10;
				BigInteger m = new BigInteger("203213123");
				BigInteger L = new BigInteger("30");
				
				PerdersonVssFactory dkgSupplier = new PerdersonVssFactory(g, h, t, n, p, lowerBound, upperBound);
				List<PerdersonVss> dkgPoints = Stream.generate(dkgSupplier)
						.limit(n)
						.collect(Collectors.toList());
				
				List< List<Integer> > quals =  dkgSupplier.calCollectedQuals(dkgPoints);
				List<BigInteger>  secret1s = dkgSupplier.calCollectedSecret1(quals, dkgPoints);
				List<BigInteger> secret2s = dkgSupplier.calCollectedSecret2(quals, dkgPoints);
				List<BigInteger> publicVals = dkgSupplier.calCollectedPublicVal(quals, dkgPoints);

				
				IntStream.range(0, dkgPoints.size())
				.boxed()
				.parallel()
				.forEach(i-> {
					dkgPoints.get(i).setFinalSecret1(secret1s.get(i));
					dkgPoints.get(i).setFinalSecret2(secret2s.get(i));
					dkgPoints.get(i).setFinalPublicVal(publicVals.get(i));
				});

				// encryption
				List<BigInteger> ciphertext =  dkgSupplier.encrypt(m,L,publicVals.get(0));

				BigInteger decrptedText = dkgSupplier.decrypt(ciphertext,dkgPoints,quals.get(0));

				if(decrptedText.equals(m)) {
					System.out.println("encry decrpt works");
				} else {
					System.out.println("encry decrpt not works");
				}

	}
}
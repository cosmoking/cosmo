/*******************************************************************************
 * Copyright 2012 Jack Wang
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cosmo.common.util;

import java.util.concurrent.atomic.AtomicLong;

import org.cosmo.common.statistics.Clock;


public class Bits
{
	private static long[] Mask;
	private static long[] InverseMask;
	private static long[] RangeCheck;
	static {
		Mask = new long[64];
		InverseMask = new long[64];
		for (int i = 1; i < 64; i++) {
			Mask[i] = (long)-1 >>> 64 - i;
			InverseMask[i] = ~Mask[i];
			//System.out.println(binStr(Mask[i]));
			//System.out.println(binStr(InverseMask[i]));
			//System.out.println(i + ":" + Mask[i]);
		}
		RangeCheck = Mask;
	}


	public static long fastSetBits (int posFromRight, int length,  int value, long bits)
	{
		return (bits >> posFromRight & InverseMask[length] | value) << posFromRight | Mask[posFromRight] & bits;
	}

	public static long fastSetBits (int posFromRight, int length,  long value, long bits)
	{
		return (bits >> posFromRight & InverseMask[length] | value) << posFromRight | Mask[posFromRight] & bits;
	}


	public static long setBits (int posFromRight, int length,  int value, long bits)
	{
		if (value > RangeCheck[length]) {
			throw new RuntimeException(New.str("Maximum value is [", RangeCheck[length], "] but get [", value, "]"));
		}
		return (bits >> posFromRight & InverseMask[length] | value) << posFromRight | Mask[posFromRight] & bits;
	}

		// about 25% overhead for range check
	public static long setBits (int posFromRight, int length,  long value, long bits)
	{
		if (value > RangeCheck[length]) {
			throw new RuntimeException(New.str("Maximum value is [", RangeCheck[length], "] but get [", value, "]"));
		}
		/*
		// ie. goal from "010 10 101010"  set  "--- 01 ------"  result "010 01 101010"

		// create mask "000 00 101010" first for later use
		long rightMaskBits = Mask[posFromRight] & bits;

		// shift right posFromRight and get "000000 010 10"
		bits = bits >> posFromRight;

		// create mask "000000 010 00"  that resets original value
		bits = InverseMask[length] & bits;

		// or value "000000 010 01"  that sets new value
		bits = bits | value;

		// shift left "010 01 000000"  to original bit alignment
		bits = bits << posFromRight;

		// or "000 00 101010" with "010 01 000000" result "010 01 101010"
		bits = bits | rightMaskBits;

		return bits;
		*/

		return (bits >> posFromRight & InverseMask[length] | value) << posFromRight | Mask[posFromRight] & bits;
	}

	public static long getBits (int posFromRight, int length, long bits)
	{
		return bits >> posFromRight & Mask[length];
	}


    public static String binStr (long i)
    {
        final char[] digits = {'0' , '1' , '2' , '3' , '4' , '5' , '6' , '7' , '8' , '9' };


    	char[] buf = new char[63];
    	int charPos = 63;
    	int radix = 1 << 1;
    	long mask = radix - 1;
    	do {
    	    buf[--charPos] = digits[(int)(i & mask)];
    	    i >>>= 1;
    	} while (charPos > 0);
    	return new String(buf);
    }


	private static class Spec
	{
		int _posFromRight;
		int _noOfBits;
	}



	public static class BooleanSpec extends Spec
	{
		public boolean get (long bits)
		{
			return Bits.getBits(_posFromRight, _noOfBits, bits) == 1 ? true : false;
		}

		public long set (long bits, boolean value)
		{
			return Bits.setBits(_posFromRight, _noOfBits, value ? 1 : 0, bits);
		}

		public final boolean get (Atomic bits)
		{
			return get(bits.get());
		}

		public final void set (Atomic bits, boolean update)
		{
			bits.set(set(bits.get(), update));
		}

		public final boolean cas (Atomic bits, boolean expect, boolean update)
		{
        	long currentBits = bits.get();
        	if (get(currentBits) == expect) {
        		return bits.compareAndSet(currentBits, set(currentBits, update));
        	}
        	return false;
		}

		public final boolean flip (Atomic bits)
		{
		    while (true) {
		        boolean current = get(bits);
		        boolean next = !current;
		        if (cas(bits, current, next)) {
		            return current;
		        }
		    }
		}
	}

	public static class ByteSpec extends Spec
	{
		public byte get (long bits)
		{
			return (byte)Bits.getBits(_posFromRight, _noOfBits, bits);
		}

		public long set (long bits, int value)  // byte
		{
			return Bits.setBits(_posFromRight, _noOfBits, value, bits);
		}

		public final byte get (Atomic bits)
		{
			return get(bits.get());
		}

		public final void set (Atomic bits, int value) // byte
		{
			bits.set(set(bits.get(), value));
		}

		public final boolean cas (Atomic bits, int expect, int update) // byte
		{
        	long currentBits = bits.get();
        	if (get(currentBits) == expect) {
        		return bits.compareAndSet(currentBits, set(currentBits, update));
        	}
        	return false;
		}

		public final long inc (Atomic bits)
		{
		    while (true) {
		        int current = get(bits);
		        int next = current + 1;
		        if (cas(bits, current, next)) {
		            return current;
		        }
		    }
		}
	}


	public static class ShortSpec extends Spec
	{
		public short get (long bits)
		{
			return (short)Bits.getBits(_posFromRight, _noOfBits, bits);
		}

		public long set (long bits, int value) // short
		{
			return Bits.setBits(_posFromRight, _noOfBits, value, bits);
		}

		public final short get (Atomic bits)
		{
			return get(bits.get());
		}

		public final void set (Atomic bits, int value) // short
		{
			bits.set(set(bits.get(), value));
		}

		public final boolean cas (Atomic bits, int expect, int update) // short
		{
        	long currentBits = bits.get();
        	if (get(currentBits) == expect) {
        		return bits.compareAndSet(currentBits, set(currentBits, update));
        	}
        	return false;
		}

		public final long inc (Atomic bits)
		{
		    while (true) {
		        int current = get(bits);
		        int next = current + 1;
		        if (cas(bits, current, next)) {
		            return current;
		        }
		    }
		}
	}




	public static class IntSpec extends Spec
	{
		public int get (long bits)
		{
			return (int)Bits.getBits(_posFromRight, _noOfBits, bits);
		}

		public long set (long bits, int value)
		{
			return Bits.setBits(_posFromRight, _noOfBits, value, bits);
		}

		public final int get (Atomic bits)
		{
			return get(bits.get());
		}

		public final void set (Atomic bits, int value)
		{
			bits.set(set(bits.get(), value));
		}

		public final boolean cas (Atomic bits, int expect, int update)
		{
        	long currentBits = bits.get();
        	if (get(currentBits) == expect) {
        		return bits.compareAndSet(currentBits, set(currentBits, update));
        	}
        	return false;
		}

		public final long inc (Atomic bits)
		{
		    while (true) {
		        int current = get(bits);
		        int next = current + 1;
		        if (cas(bits, current, next)) {
		            return current;
		        }
		    }
		}
	}

	public static class LongSpec extends Spec
	{

		public long get (long bits)
		{
			return Bits.getBits(_posFromRight, _noOfBits, bits);
		}

		public long set (long bits, long value)
		{
			return Bits.setBits(_posFromRight, _noOfBits, value, bits);
		}

		public final long get (Atomic bits)
		{
			return get(bits.get());
		}

		public final void set (Atomic bits, long update)
		{
			bits.set(set(bits.get(), update));
		}

		public final boolean cas (Atomic bits, long expect, long update)
		{
        	long currentBits = bits.get();
        	if (get(currentBits) == expect) {
        		return bits.compareAndSet(currentBits, set(currentBits, update));
        	}
        	return false;
		}

		public final long inc (Atomic bits)
		{
		    while (true) {
		        long current = get(bits);
		        long next = current + 1;
		        if (cas(bits, current, next)) {
		            return current;
		        }
		    }
		}
	}



	public static class Meta
	{
		int _totalBits;

		Spec defn (int noOfBits, Class<? extends Bits.Spec> clazz)
		{
			try {
				Spec spec = clazz.newInstance();
				spec._posFromRight = 63 - _totalBits - noOfBits;
				spec._noOfBits = noOfBits;
				_totalBits = _totalBits + noOfBits;
				if (_totalBits > 63) {
					throw new IllegalArgumentException("Length can not go beyond 63");
				}
				return spec;
			}
			catch (InstantiationException e) {
				throw new IllegalArgumentException(e);
			}
			catch (IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public BooleanSpec booleanSpec ()
		{
			return (BooleanSpec)defn(1, BooleanSpec.class);
		}

		public ByteSpec byteSpec ()
		{
			return (ByteSpec)defn(8, LongSpec.class);
		}

		public ByteSpec byteSpec (int noOfBits)
		{
			return (ByteSpec)defn(noOfBits, LongSpec.class);
		}

		public ShortSpec shortSpec ()
		{
			return (ShortSpec)defn(16, ShortSpec.class);
		}

		public ShortSpec shortSpec (int noOfBits)
		{
			return (ShortSpec)defn(noOfBits, ShortSpec.class);
		}

		public IntSpec intSpec ()
		{
			return (IntSpec)defn(32, IntSpec.class);
		}

		public IntSpec intSpec (int noOfBits)
		{
			return (IntSpec)defn(noOfBits, IntSpec.class);
		}

		public LongSpec longSpec (int noOfBits)
		{
			return (LongSpec)defn(noOfBits, LongSpec.class);
		}
	}

	public static class Atomic extends AtomicLong
	{
		public Atomic ()
		{
			this(0);
		}

		public Atomic (long value)
		{
			super(value);
		}


	}


	public static final Bits.Meta ContentMeta = new Bits.Meta();
	public static final Bits.BooleanSpec HasPictures = ContentMeta.booleanSpec();
	public static final Bits.ShortSpec NoOfWords = ContentMeta.shortSpec(3);
	public static final Bits.IntSpec TimesRead = ContentMeta.intSpec(5);
	public static final Bits.LongSpec TimesLiked = ContentMeta.longSpec(5);


	public static void main (String[] args) throws Exception
	{
		long bits = 0;

		bits = HasPictures.set(bits, true);
		bits = NoOfWords.set(bits, 4);
		bits = TimesRead.set(bits, 4);
		bits = TimesLiked.set(bits, 4);

		Clock.timer().markAndCheckRunning(System.out);
		for (int i = 0; i < 100000 ; i++) {
			HasPictures.set(bits, true);
			NoOfWords.set(bits, 4);
			TimesRead.set(bits, 4);
			TimesLiked.set(bits, 4);

		}
		Clock.timer().markAndCheckRunning(System.out);


		System.out.println(HasPictures.get(bits));
		System.out.println(NoOfWords.get(bits));
		System.out.println(TimesRead.get(bits));
		System.out.println(TimesLiked.get(bits));




		Bits.Atomic bitsAtomic = new Bits.Atomic();
		HasPictures.set(bitsAtomic, false);
		NoOfWords.set(bitsAtomic, 1);
		TimesRead.set(bitsAtomic, 1);
		TimesLiked.set(bitsAtomic, 1);

		Clock.timer().markAndCheckRunning(System.out);
		for (int i = 0; i < 10000000 ; i++) {
			HasPictures.set(bitsAtomic, false);
			NoOfWords.set(bitsAtomic, 1);
			TimesRead.set(bitsAtomic, 1);
			TimesLiked.set(bitsAtomic, 2);

		}
		Clock.timer().markAndCheckRunning(System.out);

		System.out.println(HasPictures.get(bitsAtomic));
		System.out.println(NoOfWords.get(bitsAtomic));
		System.out.println(TimesRead.get(bitsAtomic));
		System.out.println(TimesLiked.get(bitsAtomic));

	}

	/*
	public static void main (String[] args) throws Exception
	{
		Bits b = new Bits();
		b.set(6, 4, Integer.MAX_VALUE);
		b.set(13, 7 , 7);
		b.set(30, 7 , 7);
	    System.out.println(b);



	}
	*/

}







/*
1:1
2:3
3:7
4:15
5:31
6:63
7:127
8:255
9:511
10:1023
11:2047
12:4095
13:8191
14:16383
15:32767
16:65535
17:131071
18:262143
19:524287
20:1048575
21:2097151
22:4194303
23:8388607
24:16777215
25:33554431
26:67108863
27:134217727
28:268435455
29:536870911
30:1073741823
31:2147483647
32:4294967295
33:8589934591
34:17179869183
35:34359738367
36:68719476735
37:137438953471
38:274877906943
39:549755813887
40:1099511627775
41:2199023255551
42:4398046511103
43:8796093022207
44:17592186044415
45:35184372088831
46:70368744177663
47:140737488355327
48:281474976710655
49:562949953421311
50:1125899906842623
51:2251799813685247
52:4503599627370495
53:9007199254740991
54:18014398509481983
55:36028797018963967
56:72057594037927935
57:144115188075855871
58:288230376151711743
59:576460752303423487
60:1152921504606846975
61:2305843009213693951
62:4611686018427387903
63:9223372036854775807

*/









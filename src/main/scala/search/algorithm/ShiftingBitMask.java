package search.algorithm;

import java.util.Arrays;

public final class ShiftingBitMask implements SearchAlgorithm {

  private final long[] bitMasks = new long[257];
  private final long successBitMask;
  private final long unrolledSuccessBitMask;
  private final int needleLength;

  private final static class Processor implements UnrolledSearchProcessor {

    private final long[] bitMasks;
    private final long successBitMask;
    private final long unrolledSuccessBitMask;
    private final int needleLength;

    private long currentMask;
    private long previouslyFound;

    private Processor(long[] bitMasks, long successBitMask, long unrolledSuccessBitMask, int needleLength) {
      this.bitMasks = bitMasks;
      this.successBitMask = successBitMask;
      this.unrolledSuccessBitMask = unrolledSuccessBitMask;
      this.needleLength = needleLength;
    }

    @Override
    public boolean process(byte value) {
      currentMask = ((currentMask << 1) | 1) & bitMasks[Byte.toUnsignedInt(value)];
      return (currentMask & successBitMask) == 0;
    }

    @Override
    public boolean process(long value) {

      currentMask = ((currentMask << 8) | 255) & bitMasks[256] &
          ((bitMasks[(int) (value       ) & 0xFF] << 7) | 127) &
          ((bitMasks[(int) (value >>>  8) & 0xFF] << 6) |  63) &
          ((bitMasks[(int) (value >>> 16) & 0xFF] << 5) |  31) &
          ((bitMasks[(int) (value >>> 24) & 0xFF] << 4) |  15) &
          ((bitMasks[(int) (value >>> 32) & 0xFF] << 3) |   7) &
          ((bitMasks[(int) (value >>> 40) & 0xFF] << 2) |   3) &
          ((bitMasks[(int) (value >>> 48) & 0xFF] << 1) |   1) &
            bitMasks[(int) (value >>> 56)];

      final long result = currentMask & unrolledSuccessBitMask;

      if (result == 0) return true;
      else {
        previouslyFound = result;
        return false;
      }
    }

    @Override
    public int needleLength() {
      return needleLength;
    }

    @Override
    public void reset() {
      currentMask = 0;
      previouslyFound = 0;
    }

    @Override
    public boolean hasPreviouslyFound() {
      return previouslyFound != 0;
    }

    @Override
    public int nextOffset() {
      assert previouslyFound != 0;

      final int offset = 64 - Long.numberOfLeadingZeros(previouslyFound);
      previouslyFound ^= 1L << (offset - 1);

      return offset;
    }

  }

  private ShiftingBitMask(byte[] needle) {

    if (needle.length > 57) {
      throw new IllegalArgumentException("Maximum supported search pattern length is 57, got " + needle.length);
    }

    final long initial = -(1L << (needle.length));
    Arrays.fill(bitMasks, initial);

    long bit = 1L;
    for (byte c : needle) {
      bitMasks[Byte.toUnsignedInt(c)] |= bit;
      bit <<= 1;
    }
    bitMasks[256] = -1L;

    successBitMask = 1L << (needle.length - 1);
    unrolledSuccessBitMask = 255L << (needle.length - 1);
    needleLength = needle.length;
  }

  @Override
  public Processor newProcessor() {
    return new Processor(bitMasks, successBitMask, unrolledSuccessBitMask, needleLength);
  }

  public static ShiftingBitMask init(byte[] needle) {
    return new ShiftingBitMask(needle);
  }

}

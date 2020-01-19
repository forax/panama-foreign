package fr.umlv.foreign;

import static java.nio.ByteOrder.nativeOrder;
import static jdk.incubator.foreign.MemoryHandles.varHandle;
import static jdk.incubator.foreign.MemoryHandles.withStride;
import static jdk.incubator.foreign.MemoryLayout.ofSequence;
import static jdk.incubator.foreign.MemoryLayout.ofValueBits;
import static jdk.incubator.foreign.MemoryLayout.PathElement.*;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;

import org.openjdk.jmh.runner.options.OptionsBuilder;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import sun.misc.Unsafe;

// /path/to/jdk-15-foreign/bin/java --module-path target/test/artifact:deps -m fr.umlv.foreign/fr.umlv.foreign.WriteGlobalMemoryBenchMark
@SuppressWarnings("static-method")
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ReadGlobalMemoryBenchMark {
  private static final Unsafe UNSAFE;
  static {
    try {
      Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      UNSAFE = (Unsafe) unsafeField.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  // private static final VarHandle INT_HANDLE = ofValueBits(4,
  // nativeOrder()).varHandle(int.class);
  // private static final VarHandle INT_ARRAY_HANDLE = ofSequence(ofValueBits(4,
  // nativeOrder())).varHandle(int.class, sequenceElement());

  private static final VarHandle INT_HANDLE = varHandle(int.class, nativeOrder());
  private static final VarHandle INT_ARRAY_HANDLE = withStride(varHandle(int.class, nativeOrder()), 4);

  private static final ByteBuffer BYTE_BUFFER;
  private static final long ADDRESS;
  private static final MemoryAddress BASE;
  static {
    BYTE_BUFFER = ByteBuffer.allocateDirect(8192).order(nativeOrder());
    ADDRESS = UNSAFE.allocateMemory(8192);
    BASE = MemorySegment.allocateNative(8192).baseAddress();
  }

  @Benchmark
  public void bytebuffer_read(Blackhole blackhole) {
    var sum = 0;
    for (int i = 0; i < 1024; i++) {
      sum += BYTE_BUFFER.getInt(i * 4);
    }
    blackhole.consume(sum);
  }

  @Benchmark
  public void unsafe_read(Blackhole blackhole) {
    var sum = 0;
    for (var i = 0; i < 1024; i++) {
      sum += UNSAFE.getInt(ADDRESS + (i * 4));
    }
    blackhole.consume(sum);
  }

  @Benchmark
  public void segment_intArrayHandle_read(Blackhole blackhole) {
    var sum = 0;
    for (var i = 0; i < 1024; i++) {
      sum += (int)INT_ARRAY_HANDLE.get(BASE, (long) i);
    }
    blackhole.consume(sum);
  }

  @Benchmark
  public void segment_intHandle_read(Blackhole blackhole) {
    var sum = 0;
    for (var i = 0; i < 1024; i++) {
      sum += (int)INT_HANDLE.get(BASE.addOffset(i * 4));
    }
    blackhole.consume(sum);
  }
  
  public static void main(String[] args) throws RunnerException {
    var opt = new OptionsBuilder().include(ReadGlobalMemoryBenchMark.class.getName()).build();
    new Runner(opt).run();
  }
}

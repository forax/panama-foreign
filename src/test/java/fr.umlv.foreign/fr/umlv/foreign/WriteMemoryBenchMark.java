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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;

import org.openjdk.jmh.runner.options.OptionsBuilder;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import sun.misc.Unsafe;

// /path/to/jdk-15-foreign/bin/java --module-path target/test/artifact:deps -m fr.umlv.foreign/fr.umlv.foreign.WriteMemoryBenchMark
@SuppressWarnings("static-method")
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class WriteMemoryBenchMark {
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

  //private static final VarHandle INT_HANDLE = ofValueBits(32, nativeOrder()).varHandle(int.class);
  //private static final VarHandle INT_ARRAY_HANDLE = ofSequence(ofValueBits(32, nativeOrder())).varHandle(int.class, sequenceElement());

  private static final VarHandle INT_HANDLE = varHandle(int.class, nativeOrder());
  private static final VarHandle INT_ARRAY_HANDLE = withStride(varHandle(int.class, nativeOrder()), 4);
  
  @Benchmark
  public void bytebuffer_write() {
    var byteBuffer = ByteBuffer.allocateDirect(8192).order(nativeOrder());
    try {
      for (int i = 0; i < 1024; i++) {
        byteBuffer.putInt(i * 4 , 42);
      }
    } finally {
      UNSAFE.invokeCleaner(byteBuffer);
    }
  }

  @Benchmark
  public void unsafe_noclean_write() {
    var address = UNSAFE.allocateMemory(8192);
    try {
      for (var i = 0; i < 1024; i++) {
        UNSAFE.putInt(address + (i * 4) , 42);
      }
    } finally {
      UNSAFE.freeMemory(address);
    }
  }
  
  @Benchmark
  public void unsafe_clean_write() {
    var address = UNSAFE.allocateMemory(8192);
    UNSAFE.setMemory(address, 8192, (byte)0);
    try {
      for (var i = 0; i < 1024; i++) {
        UNSAFE.putInt(address + (i * 4) , 42);
      }
    } finally {
      UNSAFE.freeMemory(address);
    }
  }

  @Benchmark
  public void segment_intArrayHandle_write() {
    try(var segment = MemorySegment.allocateNative(8192)) {
      var base = segment.baseAddress();
      for (var i = 0; i < 1024; i++) {
        INT_ARRAY_HANDLE.set(base, (long) i, 42);
      }
    }
  }

  @Benchmark
  public void segment_intHandle_write() {
    try(var segment = MemorySegment.allocateNative(8192)) {
      var base = segment.baseAddress();
      for (var i = 0; i < 1024; i++) {
        INT_HANDLE.set(base.addOffset(i * 4), 42);
      }
    }
  }

  public static void main(String[] args) throws RunnerException {
    var opt = new OptionsBuilder()
        .include(WriteMemoryBenchMark.class.getName())
        .build();
    new Runner(opt).run();
  }
}

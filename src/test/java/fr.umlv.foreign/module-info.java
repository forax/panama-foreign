open module fr.umlv.foreign {
  requires org.junit.jupiter.api;
  
  requires org.openjdk.jmh;  // JMH support
  requires org.openjdk.jmh.generator;
  
  requires jdk.unsupported;  // for sun.misc.Unsafe
}
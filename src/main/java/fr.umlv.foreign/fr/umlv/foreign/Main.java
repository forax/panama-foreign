package fr.umlv.foreign;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import jdk.incubator.foreign.MemoryLayout;

public class Main {
  public static void main(String[] args) {
    VarHandle intHandle = MemoryLayout.ofValueBits(32, ByteOrder.nativeOrder()).varHandle(int.class);
    System.out.println(intHandle);
  }
}

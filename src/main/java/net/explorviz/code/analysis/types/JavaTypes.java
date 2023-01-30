package net.explorviz.code.analysis.types;

/**
 * Class for holding lists of type names.
 */
public class JavaTypes {
  public static final String[] BUILT_INS = {"Appendable", "AutoCloseable", "CharSequence",
      "Cloneable", "Comparable", "Iterable", "Readable", "Runnable",
      "Thread.UncaughtExceptionHandler", "Boolean", "Byte", "Character", "Class", "ClassLoader",
      "ClassValue", "Compiler", "Double", "Enum", "Float", "InheritableThreadLocal", "Integer",
      "Long", "Math", "Number", "Object", "Package", "Process", "ProcessBuilder", "Runtime",
      "RuntimePermission", "SecurityManager", "Short", "StackTraceElement", "StrictMath", "String",
      "StringBuffer", "StringBuilder", "System", "Thread", "ThreadGroup", "ThreadLocal",
      "Throwable", "Void", "Character.UnicodeScript", "ProcessBuilder.Redirect.Type",
      "Thread.State", "ArithmeticException", "ArrayIndexOutOfBoundsException",
      "ArrayStoreException", "ClassCastException", "ClassNotFoundException",
      "CloneNotSupportedException", "EnumConstantNotPresentException", "Exception",
      "IllegalAccessException", "IllegalArgumentException", "IllegalMonitorStateException",
      "IllegalStateException", "IllegalThreadStateException", "IndexOutOfBoundsException",
      "InstantiationException", "InterruptedException", "NegativeArraySizeException",
      "NoSuchFieldException", "NoSuchMethodException", "NullPointerException",
      "NumberFormatException", "ReflectiveOperationException", "RuntimeException",
      "SecurityException", "StringIndexOutOfBoundsException", "TypeNotPresentException",
      "UnsupportedOperationException", "AbstractMethodError", "AssertionError",
      "BootstrapMethodError", "ClassCircularityError", "ClassFormatError", "Error",
      "ExceptionInInitializerError", "IllegalAccessError", "IncompatibleClassChangeError",
      "InstantiationError", "InternalError", "LinkageError", "NoClassDefFoundError",
      "NoSuchFieldError", "NoSuchMethodError", "OutOfMemoryError", "StackOverflowError",
      "ThreadDeath", "UnknownError", "UnsatisfiedLinkError", "UnsupportedClassVersionError",
      "VerifyError", "VirtualMachineError", "Deprecated", "Override", "SafeVarargs",
      "SuppressWarnings"};

  public static final String[] PRIMITIVES = {"void", "byte", "short", "int", "long", "float",
      "double", "boolean", "char"};
}

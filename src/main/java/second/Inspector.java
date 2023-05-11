package second;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Frank Mayer, Antonia Friese, René Ott
 * @version 1.0 2023-05-11
 */
public class Inspector {

  private static final String indent = "    ";

  public static void main(final String[] args) {
    String className;
    try (var scanner = new Scanner(System.in)) {
      while (true) {
        // input class name
        System.out.print("Enter class name or 'exit' to exit: ");
        className = scanner.nextLine().trim();
        if (className.equals("exit")) {
          break;
        }

        // inspect class
        try {
          final var clazz = Class.forName(className);
          final var inspector = new Inspector(clazz);
          System.out.println("\n");
          System.out.println(inspector);
        } catch (final ClassNotFoundException e) {
          System.out.println("Class not found: " + className);
        }
      }
    }
  }

  // sort members
  private static int memberComparator(final Member a, final Member b) {
    // static first
    final var aStatic = Modifier.isStatic(a.getModifiers());
    final var bStatic = Modifier.isStatic(b.getModifiers());
    if (aStatic != bStatic) {
      return Boolean.compare(bStatic, aStatic);
    }

    // sort by publicity first (public, protected, private)
    final var aMod = a.getModifiers();
    final var bMod = b.getModifiers();
    if (aMod != bMod) {
      return Integer.compare(aMod, bMod);
    }

    // sort by name
    return a.getName().compareTo(b.getName());
  }

  // sort parameters
  private static int fieldComparator(final Field a, final Field b) {
    final var memberSorting = Inspector.memberComparator(a, b);
    if (memberSorting != 0) {
      return memberSorting;
    }

    // sort by type
    final var aType = a.getType();
    final var bType = b.getType();
    final var cmp = aType.getName().compareTo(bType.getName());
    return cmp;
  }

  // sort constructor methods
  private static int constructorComparator(final Constructor<?> a, final Constructor<?> b) {
    // sort by publicity first (public, protected, private)
    final var aMod = a.getModifiers();
    final var bMod = b.getModifiers();
    if (aMod != bMod) {
      return Integer.compare(aMod, bMod);
    }

    // if the publicity is the same, sort by number of parameters
    final var aParams = a.getParameterCount();
    final var bParams = b.getParameterCount();
    if (aParams != bParams) {
      return Integer.compare(aParams, bParams);
    }

    // if the number of parameters is the same, sort by parameter types
    final var aTypes = a.getParameterTypes();
    final var bTypes = b.getParameterTypes();

    for (var i = 0; i < aTypes.length; ++i) {
      final var aType = aTypes[i];
      final var bType = bTypes[i];
      final var cmp = aType.getName().compareTo(bType.getName());
      if (cmp != 0) {
        return cmp;
      }
    }

    return 0;
  }

  // sort methods
  private static int methodComparator(final Method a, final Method b) {
    final var memberSorting = Inspector.memberComparator(a, b);
    if (memberSorting != 0) {
      return memberSorting;
    }

    // sort by return type
    final var aType = a.getReturnType();
    final var bType = b.getReturnType();
    final var cmp = aType.getName().compareTo(bType.getName());
    if (cmp != 0) {
      return cmp;
    }

    // sort by number of parameters
    final var aParams = a.getParameterCount();
    final var bParams = b.getParameterCount();
    if (aParams != bParams) {
      return Integer.compare(aParams, bParams);
    }

    // sort by parameter types
    final var aTypes = a.getParameterTypes();
    final var bTypes = b.getParameterTypes();

    for (var i = 0; i < aTypes.length; ++i) {
      final var aParam = aTypes[i];
      final var bParam = bTypes[i];
      final var cmp2 = aParam.getName().compareTo(bParam.getName());
      if (cmp2 != 0) {
        return cmp2;
      }
    }

    return 0;
  }

  // store needed imports by other methods
  private final SortedSet<String> imports = new TreeSet<>();

  // the class to inspect
  private final Class<?> clazz;

  // store the stringified version of the class for later use
  private String string;

  public Inspector(final Class<?> clazz) {
    this.clazz = clazz;
  }

  // build java like code using reflection
  @Override
  public String toString() {
    if (this.string != null) {
      return this.string;
    }

    final StringBuilder sb = new StringBuilder();

    sb.append(this.getPackage()).append('\n');

    final var fields = this.getFields();
    final var constructors = this.getConstructors();
    final var methods = this.getMethods();

    // create class head
    final var headSB = new StringBuilder();
    headSB.append(Modifier.toString(this.clazz.getModifiers()));
    if (this.clazz.isInterface()) {
      headSB.append(" interface ");
    } else if (this.clazz.isEnum()) {
      headSB.append(" enum ");
    } else if (this.clazz.isAnnotation()) {
      headSB.append(" annotation ");
    } else {
      headSB.append(" class ");
    }
    headSB.append(this.clazz.getSimpleName());

    final var interfaces = this.clazz.getInterfaces();
    if (interfaces.length > 0) {
      headSB.append(" implements ");
      for (var i = 0; i < interfaces.length; ++i) {
        final var interf = interfaces[i];
        this.addImport(interf);
        headSB.append(interf.getSimpleName());
        if (i < interfaces.length - 1) {
          headSB.append(", ");
        }
      }
    }

    final var superclass = this.clazz.getSuperclass();
    if (superclass != null) {
      this.addImport(superclass);
      headSB.append(" extends ").append(superclass.getSimpleName());
    }

    headSB.append(" {\n\n");

    // end of head

    // add imports
    // get imports at the end because the other methods may add some
    // dependencies to the imports
    sb.append(this.getImports()).append("\n\n");

    // add previously created stuff
    sb.append(headSB);
    sb.append(Inspector.indent + "// Fields\n").append(fields).append('\n');
    sb.append(Inspector.indent + "// Constructors\n").append(constructors).append('\n');
    sb.append(Inspector.indent + "// Methods\n").append(methods).append('\n');
    sb.append('}');

    return this.string = sb.toString();
  }

  // get the package name for the class
  private String getPackage() {
    return "package " + this.clazz.getPackage().getName() + ";\n";
  }

  // get all needed imports
  private String getImports() {
    final var sb = new StringBuilder();
    for (final var imp : this.imports) {
      sb.append("import " + imp + ";\n");
    }
    return sb.toString();
  }

  // get all constructors
  private String getConstructors() {
    final var sb = new StringBuilder();
    final var className = this.clazz.getSimpleName();
    final var constructors =
        Arrays.stream(this.clazz.getDeclaredConstructors())
            .sorted(Inspector::constructorComparator)
            .toList();
    for (final var constructor : constructors) {
      sb.append(Inspector.indent);

      final var mod = Modifier.toString(constructor.getModifiers());
      if (!mod.isEmpty()) {
        sb.append(mod).append(' ');
      }

      sb.append(className).append('(');
      final Parameter[] parameters = constructor.getParameters();
      for (var i = 0; i < parameters.length; ++i) {
        this.addImport(parameters[i].getType());
        final Parameter parameter = parameters[i];
        sb.append(parameter.getType().getSimpleName() + ' ' + parameter.getName());
        if (i < parameters.length - 1) {
          sb.append(", ");
        }
      }
      sb.append(");\n");
    }
    return sb.toString();
  }

  // get all methods
  private String getMethods() {
    final var sb = new StringBuilder();
    final var methods =
        Arrays.stream(this.clazz.getDeclaredMethods()).sorted(Inspector::methodComparator).toList();
    for (final var method : methods) {
      final var ano = method.getAnnotations();
      if (ano.length > 0) {
        for (final var annotation : ano) {
          sb.append(Inspector.indent).append(Inspector.indent).append(annotation).append('\n');
          this.addImport(annotation.annotationType());
        }
      }
      this.addImport(method.getReturnType());
      sb.append(Inspector.indent);

      final var mod = Modifier.toString(method.getModifiers());
      if (!mod.isEmpty()) {
        sb.append(mod).append(' ');
      }

      sb.append(method.getReturnType().getSimpleName())
          .append(' ')
          .append(method.getName())
          .append('(');

      final Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        final Parameter parameter = parameters[i];
        sb.append(parameter.getType().getSimpleName()).append(' ').append(parameter.getName());
        if (i < parameters.length - 1) {
          sb.append(", ");
        }
      }
      sb.append(");\n");
    }

    return sb.toString();
  }

  // get all fields
  private String getFields() {
    final var sb = new StringBuilder();
    final var fields =
        Arrays.stream(this.clazz.getDeclaredFields()).sorted(Inspector::fieldComparator).toList();
    for (final Field field : fields) {
      this.addImport(field.getType());
      final var mod = Modifier.toString(field.getModifiers());
      sb.append(Inspector.indent);
      if (!mod.isEmpty()) {
        sb.append(mod).append(' ');
      }
      sb.append(field.getType().getSimpleName()).append(' ').append(field.getName()).append(";\n");
    }
    return sb.toString();
  }

  // add dependency to the imports
  private void addImport(final Class<?> t) {
    // primitive values
    if (t.isPrimitive()) {
      return;
    }

    // array
    if (t.isArray()) {
      this.addImport(t.getComponentType());
      return;
    }

    this.addImport(t.getPackageName() + '.' + t.getSimpleName());

    // generic
    for (final var genT : t.getTypeParameters()) {
      for (final var bound : genT.getBounds()) {
        this.addImport(bound);
      }
    }
  }

  // add dependency to the imports
  private void addImport(final String type) {
    this.imports.add(type);
  }

  // add dependency to the imports
  private void addImport(final Type t) {
    if (t instanceof Class) {
      this.addImport((Class<?>) t);
    }

    if (t instanceof WildcardType) {
      for (final var bound : ((WildcardType) t).getUpperBounds()) {
        this.addImport(bound);
      }
    }
  }
}
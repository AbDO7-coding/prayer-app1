import javax.tools.*;
import java.nio.file.*;
import java.util.*;

/** Usage: java tools/Compile.java OUT_DIR file1.java file2.java ...  (compiles with the JDK's built-in compiler) */
public class Compile {
    public static void main(String[] a) throws Exception {
        Files.createDirectories(Paths.get(a[0]));
        List<String> args = new ArrayList<>(List.of("-encoding", "UTF-8", "-d", a[0], "-Xlint:-options", "-Xmaxerrs", "5000"));
        args.addAll(Arrays.asList(a).subList(1, a.length));
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        int rc = jc.run(null, null, null, args.toArray(new String[0]));
        System.exit(rc);
    }
}

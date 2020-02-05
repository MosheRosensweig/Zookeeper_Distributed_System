package edu.yu.cs.fall2019.intro_to_distributed;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavaRunnerImpl implements JavaRunner
{
    //{"server", "parsing"}
    private static final String[] ON_LOG_LEVELS = {}; //probably should be an enum
    private Path path;

    public JavaRunnerImpl()
    {
        try {
            path = Files.createTempDirectory(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JavaRunnerImpl(Path path)
    {
        this.path = path;
    }


    public String compileAndRun(InputStream in) throws IllegalArgumentException, IOException
    {
        return compileAndRun(streamToString(in));
    }

    /**
     * Reads the source code from the InputStream, compiles it, and runs it. The code must not depend on any classes
     * other than those in the standard JRE. The Java class whose source code is submitted must have a no-args
     * constructor and a no-args method called "run", which is the method that will be invoked to execute the class.
     * <p>
     * See https://www.programcreek.com/java-api-examples/?api=javax.tools.JavaCompiler for related code examples
     *
     * @param in
     * @return the System.err and System.out output from running the compiled Java code
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public String compileAndRun(String in) throws IllegalArgumentException, IOException
    {
        //To .java file//
        String inputCode = in;
        String className = getClassName(inputCode);
        String packageName = getPackageName(inputCode);
        String dir = path + File.separator;
        File javaFile = codeToFile(inputCode, className, packageName, dir);

        //Compile and Gather Compilation Errors//
        DiagnosticCollector<JavaFileObject> diagnostics = compile(javaFile);
        String compilationErrors = generateCompilationErrors(diagnostics);
        //if (compilationErrors != null) return compilationErrors;
        if (compilationErrors != null) throw new IllegalArgumentException(compilationErrors);

        //Run the code with reflection//
        ByteArrayOutputStream[] out_err;
        try {
            out_err = classLoadAndRun(className, packageName, dir);
        }catch(IllegalArgumentException iae){
            throw new IllegalArgumentException(iae.getMessage());
        }catch (Exception e) {
            //Pass any issues that are thrown to the client as a 500//
            return stackTraceToString(e);
        }
        return runtimePrintOuts(out_err);
    }
    //stacktrace to string https://stackoverflow.com/questions/1149703/how-can-i-convert-a-stack-trace-to-a-string
    public static String stackTraceToString(Exception e) throws IOException
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        sw.close();
        pw.close();
        return sStackTrace;
    }

    private String runtimePrintOuts(ByteArrayOutputStream[] out_err)
    {
        //Setup//
        StringBuilder output = new StringBuilder("\nSystem.out:\n[");
        StringBuilder errors = new StringBuilder("System.err:\n[");
        String outStr = new String(out_err[0].toByteArray(), StandardCharsets.UTF_8);
        String errStr = new String(out_err[1].toByteArray(), StandardCharsets.UTF_8);
        output.append(outStr);
        errors.append(errStr);
        return errors.toString() + "]" + output.toString() + "]";
    }

    private ByteArrayOutputStream[] classLoadAndRun(String className, String packageName, String dir) throws IOException
    {
        File toLoad = new File(dir); //location of the class file
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{toLoad.toURI().toURL()});
        logTest("[2] !packageName.isEmpty() = " + !packageName.isEmpty() + ", " + packageName, "parsing");
        if(!packageName.isEmpty()) packageName += ".";

        synchronized (this) { //make sure only this program outs are caught //todo limit the critical region
            //Redirect Sys out and Sys err
            ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
            PrintStream psOut = new PrintStream(baosOut, true, "UTF-8");
            ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
            PrintStream psErr = new PrintStream(baosErr, true, "UTF-8");
            System.setOut(psOut);
            System.setErr(psErr);


            try {
                Class cls = classLoader.loadClass(packageName + className);
                Object instance = cls.getDeclaredConstructor().newInstance();
                Method run = cls.getMethod("run");

                //Run the code//
                synchronized (this) {
                    run.invoke(instance);
                }
            }catch (NoSuchMethodException nme){
                //throw new IllegalArgumentException(nme.getMessage());
                throw new IllegalArgumentException("Client code does not contain a \"run\" method ");
            }catch (Exception e) {
                //System.err.println(e.getMessage());
                System.err.println(e.getCause());
                //System.err.println(e.getCause().getMessage());
            }
            finally {
                //Reset Sys out and Sys err//
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
                psOut.close();
                psErr.close();
            }



            //Return the diagnostics//
            ByteArrayOutputStream[] out_err = new ByteArrayOutputStream[]{baosOut, baosErr};
            return out_err;
        }
    }

    /**
     * Read all of the diagnostics and convert it into a human-readable string
     * following Judah's specs "Error on line 1, column 35, in etc..."
     *
     * @param diagnostics
     * @return a string formatted as described above, or null if there were no issues
     */
    private String generateCompilationErrors(DiagnosticCollector<JavaFileObject> diagnostics)
    {
        StringBuilder diags = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
            diags.append("Error on line " + diagnostic.getLineNumber()
                    + ", column " + diagnostic.getColumnNumber()
                    + ", in " + diagnostic.getSource().toUri() + "\n");
        return diags.length() == 0 ? null : diags.toString();
    }

    /**
     * See https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html
     * and https://www.programcreek.com/java-api-examples/?api=javax.tools.JavaCompiler
     */
    private DiagnosticCollector<JavaFileObject> compile(File javaFile) throws IOException
    {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null);
             StringWriter out = new StringWriter();
             PrintWriter outWriter = new PrintWriter(out)) {
            Iterable<? extends JavaFileObject> input = fm.getJavaFileObjects(javaFile);
            boolean compile_success = compiler.getTask(null, fm, diagnostics, null, null, input).call();
        }
        return diagnostics;
    }

    /**
     * @param inputCode - the java class as a string
     * @return The file created
     * @throws FileNotFoundException
     */
    private File codeToFile(String inputCode, String className, String packageName, String rootDir) throws FileNotFoundException
    {
        packageName = packageName.replace(".", File.separator);
        String dir = rootDir;
        logTest("[1] !packageName.isEmpty() = " + !packageName.isEmpty() + ", " + packageName, "parsing");
        if(!packageName.isEmpty()) dir += packageName + File.separator;
        //Create the dirs//
        File file = new File(dir);
        file.mkdirs(); // this should make the dirs that don't exist, but if they do, then it should do nothing

        //Write the java code to the dir as a .java
        try (PrintWriter out = new PrintWriter(dir + className + ".java")) {
            out.println(inputCode);
        }
        return new File(dir + className + ".java");
    }

    //thanks https://stackoverflow.com/questions/37403641/regex-to-fetch-the-correct-java-class-name?rq=1
    public static String getClassName(String str)
    {
        Pattern p = Pattern.compile("(?<=\\n|\\A)(?:public\\s)?(class|interface|enum)\\s([^\\n\\s]*)");
        Matcher matcher = p.matcher(str);
        String c_name = "";
        if (matcher.find()) {
            logTest(matcher.group(2), "parsing");
            c_name = matcher.group(2);
        }
        return c_name;
    }

    //thanks https://stackoverflow.com/questions/7731121/is-it-possible-to-write-a-regex-to-extract-java-package
    //thanks https://stackoverflow.com/questions/1657066/java-regular-expression-finding-comments-in-code
    public static String getPackageName(String str)
    {
        //"package\\s+([\\w\\.]+);"
        Pattern p = Pattern.compile("package\\s+([\\w\\.]+);");
        String clean = str.replaceAll( "//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1 " );
        Matcher matcher = p.matcher(clean);
        String p_name = "";
        if (matcher.find()) {
            logTest(matcher.group(1), "parsing");
            p_name = matcher.group(1);
        }
        return p_name;
    }

    public static String streamToString(InputStream in)
    {
        return new BufferedReader(new InputStreamReader(in))
                .lines().collect(Collectors.joining("\n"));
    }

    public static void logTest(String str)
    {
        System.out.println("Logging --> " + str);
    }
    public static void logTest(String str, String type)
    {
        boolean print = false;
        for(String val : ON_LOG_LEVELS) if(val.equals(type)) {
            print = true;
            break;
        }
        if(print) System.out.println("Logging[" + type + "] --> " + str);
    }

}

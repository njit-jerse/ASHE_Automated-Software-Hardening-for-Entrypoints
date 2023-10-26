package edu.njit.jerse.utils;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import edu.njit.jerse.services.MethodReplacementService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class to parse and analyze Java code using the JavaParser library.
 * <p>
 * This class provides methods for extracting method signature, method bodies,
 * class declarations, and Java code blocks from a given string or file.
 */
public final class JavaCodeParser {
    private static final Logger LOGGER = LogManager.getLogger(MethodReplacementService.class);
    private static final Pattern JavaCodeBlockPattern = Pattern.compile("```java(.*?)```", Pattern.DOTALL);

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is a utility class and is not meant to be instantiated.
     * All methods are static and can be accessed without creating an instance.
     * Making the constructor private ensures that this class cannot be instantiated
     * from outside the class and helps to prevent misuse.
     * </p>
     */
    private JavaCodeParser() {
        throw new AssertionError("Cannot instantiate JavaCodeParser");
    }

    /**
     * Indicates the presence or absence of a modifier in a method signature.
     */
    public enum ModifierPresent {
        /**
         * Indicates that a modifier is present.
         */
        PRESENT,

        /**
         * Indicates that a modifier is absent.
         */
        ABSENT
    }

    /**
     * Represents the signature of a Java method.
     * This includes information about its access modifier, return type,
     * method name, and parameters.
     */
    public record MethodSignature(
            /**
             * Indicates the presence or absence of a modifier in a method signature.
             * <p>
             * Used to determine whether to add specific modifiers to a {@link MethodDeclaration}.
             * If the value is {@code ABSENT}, no modifiers are added to the method, reflecting the default
             * or package-private access. If the value is {@code PRESENT}, the associated modifiers
             * from {@code modifierKeyword()} are added to the method.
             */
            ModifierPresent modifierPresent,
            /**
             * A list of access modifiers for the method.
             * It can be empty, indicating that the method has default (package-private) access.
             */
            List<Modifier.Keyword> modifierKeyword,
            /**
             * The return type of the method.
             */
            String returnType,
            /**
             * The name of the method.
             */
            String methodName,
            /**
             * The method's parameters, formatted as a comma-separated string.
             * Each parameter comprises its type followed by its name, e.g., "int x, String y".
             * This string is typically processed further to delineate individual parameter nodes
             * when generating a {@link MethodDeclaration}.
             */
            String parameters
    ) {
    }

    /**
     * Extracts method signature from a given method string.
     *
     * @param method the method string from which to extract the signature.
     * @return an Optional containing {@link MethodSignature} if found, else empty.
     */
    public static Optional<MethodSignature> extractMethodSignature(String method) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(method);

            Optional<MethodDeclaration> methodDeclarationOpt = cu.findFirst(MethodDeclaration.class);
            if (methodDeclarationOpt.isPresent()) {
                MethodDeclaration methodDeclaration = methodDeclarationOpt.get();

                String returnType = methodDeclaration.getType().asString();
                String name = methodDeclaration.getName().asString();
                String params = extractParameters(methodDeclaration);
                List<Modifier.Keyword> modifiers = extractModifiers(methodDeclaration);
                ModifierPresent presence = determineModifierPresence(modifiers);

                LOGGER.debug("Extracted method signature: Modifiers={} ReturnType={} Name={} Parameters={}",
                        modifiers.isEmpty() ? "None" : modifiers.stream().map(Enum::name).collect(Collectors.joining(", ")), returnType, name, params);

                return Optional.of(new MethodSignature(presence, modifiers, returnType, name, params));
            }
        } catch (ParseProblemException ex) {
            LOGGER.error("Failed to parse method due to syntax error(s): {}", ex.getProblems());
        } catch (Exception ex) {
            LOGGER.error("Failed to extract method signature due to an unexpected error: ", ex);
        }
        return Optional.empty();
    }

    private static ModifierPresent determineModifierPresence(List<Modifier.Keyword> keywords) {
        return keywords.contains(Modifier.Keyword.DEFAULT) ? ModifierPresent.ABSENT : ModifierPresent.PRESENT;
    }

    /**
     * Extracts and returns the parameters from the provided method declaration
     * as a formatted string.
     *
     * @param methodDeclaration the method declaration from which to extract the parameters.
     * @return a formatted string representing the parameters of the method.
     */
    private static String extractParameters(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters()
                .stream()
                .map(p -> p.getType().asString() + " " + p.getName().asString())
                .collect(Collectors.joining(", "));
    }

    /**
     * Extracts the modifier keyword(s) from a given {@link MethodDeclaration}.
     * Returns {@link Modifier.Keyword#DEFAULT} if no specific modifier is found.
     *
     * @param methodDeclaration the method declaration to extract the modifier(s) from
     * @return the extracted modifier or {@link Modifier.Keyword#DEFAULT} if none found
     */
    private static List<Modifier.Keyword> extractModifiers(MethodDeclaration methodDeclaration) {
        List<Modifier.Keyword> keywords = methodDeclaration.getModifiers()
                .stream()
                .map(Modifier::getKeyword)
                .collect(Collectors.toList());
        if (keywords.isEmpty()) {
            keywords.add(Modifier.Keyword.DEFAULT);
        }
        return keywords;
    }

    /**
     * Extracts the method name from a provided target method string.
     * <p>
     * The expected format is: "package.name.ClassName#methodName()"
     * Parameter types must always be provided, though they can be empty if the method has no parameters.
     * For example:
     * <ul>
     *     <li>"com.example.package.MyClass#myMethod(ParamType1, ParamType2)".</li>
     *     <li>"com.example.package.MyClass#myMethod()". If the method has no parameters.</li>
     * </ul>
     * <p>
     * If the target method string does not match the expected format,
     * this method throws an {@link IllegalArgumentException}.
     *
     * @param targetMethod the target method string in the format "package.name.ClassName#methodName()".
     * @return the extracted method name from the provided target method string.
     * @throws IllegalArgumentException if the targetMethod format is invalid.
     */
    public static String extractMethodName(String targetMethod) {
        Pattern pattern = Pattern.compile("#(.*?)\\(");
        Matcher matcher = pattern.matcher(targetMethod);

        if (matcher.find()) {
            // Suppressed warning: Based on Matcher's behavior, group(1) won't be null if find() succeeds.
            // Explanation: In the context of the regex pattern "#(.*?)\\(", `group(0)` would represent the entire
            // matched substring starting from `#` and ending just before the first open parenthesis `(`.
            // So, for an input like "com.example.package.MyClass#myMethod(ParamType1, ParamType2)", `group(0)`
            // would return `#myMethod(`, while `group(1)` captures just the method name, returning `myMethod`.
            @SuppressWarnings("nullness")
            String methodName = matcher.group(1);
            if (methodName != null) {
                return methodName;
            }
        }

        throw new IllegalArgumentException("Invalid targetMethod format");
    }

    /**
     * Extracts the body of a specified method from the given Java code string.
     *
     * @param method the entire Java method as a string
     * @return the body of the method as a string, or an empty string if not found
     */
    public static String extractMethodBody(String method) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(method);
            MethodDeclaration methodDeclaration = cu.findFirst(MethodDeclaration.class).orElse(null);

            if (methodDeclaration != null && methodDeclaration.getBody().isPresent()) {
                return methodDeclaration.getBody().get().toString();
            } else {
                LOGGER.warn("Method body not found.");
            }

        } catch (Exception ex) {
            LOGGER.error("Failed to extract method body: ", ex);
        }
        return "";
    }

    /**
     * Finds the class or interface declaration containing a method with a specified name from a Java file.
     *
     * @param filePath   the path to the Java file
     * @param methodName the name of the method you're looking for
     * @return the name of the class or interface containing the specified method
     * @throws FileNotFoundException If the file cannot be read or if no such method exists
     */
    public static ClassOrInterfaceDeclaration extractClassByMethodName(String filePath, String methodName)
            throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            CompilationUnit cu = StaticJavaParser.parse(fis);
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

            ClassOrInterfaceDeclaration result =
                    classes.stream()
                            .filter(declaration -> declaration
                                    .getMethods()
                                    .stream()
                                    .anyMatch(method ->
                                            method.getNameAsString().equals(methodName)
                                    )
                            )
                            .findFirst()
                            .orElse(null);

            if (result == null) {
                LOGGER.warn("No class or interface declarations containing the method {} found in file: {}", methodName, filePath);
                throw new FileNotFoundException("No class or interface declarations containing the method " + methodName + " found in file: " + filePath);
            }

            return result;

        } catch (IOException ex) {
            LOGGER.error("Error reading file: {}", filePath, ex);
            throw new FileNotFoundException("Error reading file: " + ex.getMessage());
        }
    }

    /**
     * Extracts a Java code block enclosed in {@code ```java ... ```} from a given response string.
     *
     * @param response the response string potentially containing a Java code block
     * @return the Java code block without enclosing tags, or empty string if not found
     */
    public static String extractJavaCodeBlockFromResponse(String response) {
        Matcher matcher = JavaCodeBlockPattern.matcher(response);

        if (matcher.find()) {
            String matchedGroup = matcher.group(1);
            if (matchedGroup != null) {
                LOGGER.debug("Extracted Java code block from response: {}", matchedGroup);
                return matchedGroup.trim();
            }
        }

        return "";
    }
}

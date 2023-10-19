package edu.njit.jerse.services;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import edu.njit.jerse.utils.JavaCodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides functionality to replace Java methods within a given file.
 * <p>
 * This service uses the JavaParser library to analyze and manipulate Java source files,
 * facilitating the replacement of methods with new implementations provided as input.
 */
public class MethodReplacementService {
    private static final Logger LOGGER = LogManager.getLogger(MethodReplacementService.class);

    /**
     * Replaces an existing Java method in the specified file with a new, updated method.
     *
     * @param filePath      the path to the Java file containing the method to be replaced.
     * @param newMethodCode the new method code to replace the existing method.
     * @return {@code true} if the replacement operation was successful; {@code false} otherwise.
     */
    public boolean replaceMethodInFile(String filePath, String className, String newMethodCode) {
        LOGGER.info("Attempting to replace method in file: {}", filePath);

        Path path = Paths.get(filePath);
        JavaCodeParser javaCodeParser = new JavaCodeParser();

        Optional<JavaCodeParser.MethodSignature> methodSignatureOpt = javaCodeParser.extractMethodSignature(newMethodCode);
        if (methodSignatureOpt.isEmpty() || !isValidMethodSignature(methodSignatureOpt.get())) {
            LOGGER.error("Could not parse the provided method.");
            return false;
        }

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(path);
        } catch (Exception ex) {
            String errorMessage = (ex.getMessage() != null) ? ex.getMessage() : "Unknown error";
            LOGGER.error("Error while parsing file {}: {}", path, errorMessage);
            return false;
        }

        Optional<ClassOrInterfaceDeclaration> classOpt = getClassDeclaration(cu, className);
        if (classOpt.isEmpty()) {
            LOGGER.error("No class found in {}", path);
            return false;
        }

        MethodDeclaration newMethod = createNewMethodFromSignature(methodSignatureOpt.get(), javaCodeParser, newMethodCode);

        boolean wasMethodReplaced = replaceMethodInClassDeclaration(classOpt.get(), newMethod, methodSignatureOpt.get());
        if (!wasMethodReplaced) {
            LOGGER.error("No matching method found to replace in file: {}", filePath);
            return false;
        }

        boolean didWriteCUToFile = writeCompilationUnitToFile(path, cu);
        if (didWriteCUToFile) {
            LOGGER.info("Method replacement succeeded for file: {}", filePath);
        } else {
            LOGGER.error("Method replacement failed for file: {}", filePath);
        }
        return didWriteCUToFile;
    }

    /**
     * Replaces a method in the specified class declaration with a new method if it matches the provided method signature.
     *
     * @param classDecl       The class or interface declaration where the method replacement should be performed.
     * @param newMethod       The new method declaration that will replace the existing method if a match is found.
     * @param methodSignature The signature of the method to be replaced. Replacement is done based on this signature.
     * @return True if a method with the provided signature was found and replaced, otherwise false.
     * <p>
     * Note: If multiple methods have the same signature, only the first encountered will be replaced.
     * TODO: Add support for replacing a specific method if multiple methods have the same signature.
     * TODO: I.E. take into account method overriding.
     */
    private boolean replaceMethodInClassDeclaration(ClassOrInterfaceDeclaration classDecl, MethodDeclaration newMethod, JavaCodeParser.MethodSignature methodSignature) {
        for (MethodDeclaration method : classDecl.getMethods()) {
            // If method has the same signature as the one we want to replace, replace it
            if (doesMethodSignatureMatch(method, methodSignature)) {
                method.replace(newMethod);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the provided method matches the target method signature based on "override-equivalent" rules
     * as defined in the Java Language Specification (JLS) - <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2">§8.4.2</a>.
     * <p>
     * The match-check is performed based on:
     * 1. Method name.
     * 2. Number of parameters.
     * 3. Type of parameters (order matters).
     * 4. Return type - this deviates slightly from the JLS where only parameter types
     *                  are considered for "override-equivalent".
     * <p>
     * Parameter names and parameter default values/annotations are not considered during
     * the match. The handling of type variables needs to be verified.
     *
     * @param method          The method declaration to check against the target signature.
     * @param targetSignature The target method signature to which the provided method is compared.
     * @return True if the method matches the target signature, otherwise false.
     * <p>
     * Example Usage:
     * doesMethodSignatureMatch(someMethodDecl, new MethodSignature("methodName", "paramType1, paramType2", "returnType"));
     */
    private boolean doesMethodSignatureMatch(MethodDeclaration method, JavaCodeParser.MethodSignature targetSignature) {
        // Check if the method name matches
        if (!method.getNameAsString().equals(targetSignature.methodName())) {
            return false;
        }

        // Split the parameters string into an array and check if the parameter count matches
        List<String> targetParameterTypes = splitParameters(targetSignature.parameters());
        boolean noParameters = targetParameterTypes.size() == 1 && targetParameterTypes.get(0).isEmpty();

        if (noParameters && method.getParameters().isEmpty()) {
            // If both method and target have no parameters, they match
            return method.getTypeAsString().equals(targetSignature.returnType());
        } else if (method.getParameters().size() != targetParameterTypes.size()) {
            // If parameter counts (excluding the no parameter case) don't match, return false
            return false;
        }

        // Check if the parameter types match
        for (int i = 0; i < method.getParameters().size(); i++) {
            String actualParamType = method.getParameter(i).getType().asString();
            String expectedParamType = targetParameterTypes.get(i).split(" ")[0]; // Only compare type, not name

            if (!actualParamType.equals(expectedParamType)) {
                return false;
            }
        }

        return method.getTypeAsString().equals(targetSignature.returnType());
    }

    /**
     * Splits a parameter string into individual parameter definitions, taking into account nested
     * generic types. This method is designed to handle complex type definitions, such as {@code Map<String, Integer>}.
     * <p>
     * The method ensures that the split occurs only at the top-level commas, avoiding splits inside generic definitions.
     *
     * @param parameterString The entire parameter string that needs to be split into individual parameter definitions.
     * @return A list of individual parameter definitions split from the input string.
     */
    private static List<String> splitParameters(String parameterString) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder currentParameter = new StringBuilder();

        for (char c : parameterString.toCharArray()) {
            switch (c) {
                case '<':
                    depth++;
                    break;
                case '>':
                    depth--;
                    break;
                case ',':
                    if (depth == 0) {
                        result.add(currentParameter.toString().trim());
                        currentParameter = new StringBuilder();
                        continue;
                    }
                    break;
            }
            currentParameter.append(c);
        }
        if (!currentParameter.isEmpty()) {
            result.add(currentParameter.toString().trim());
        }

        return result;
    }

    /**
     * Checks if the extracted method signature is both complete and conforms to the expected
     * format of a valid Java method signature.
     *
     * @param signature the method signature to be checked
     * @return {@code true} if the method signature is complete and a valid Java method signature; {@code false} otherwise.
     */
    private boolean isValidMethodSignature(JavaCodeParser.MethodSignature signature) {
        boolean isValidSig =
                signature.returnType() != null &&
                        signature.methodName() != null &&
                        signature.parameters() != null;

        if (isValidSig) {
            LOGGER.debug("Java method signature is valid: returnType={}, methodName={}, parameters={}",
                    signature.returnType(), signature.methodName(), signature.parameters());
        } else {
            LOGGER.warn("Invalid Java method signature detected: returnType={}, methodName={}, parameters={}",
                    signature.returnType(), signature.methodName(), signature.parameters());
        }

        return isValidSig;
    }

    /**
     * Retrieves the declaration of a specific class or interface by name from the
     * provided compilation unit.
     *
     * @param cu        The compilation unit from which the class or interface
     *                  declaration needs to be extracted.
     * @param className The name of the class or interface whose declaration is
     *                  to be fetched.
     * @return An {@code Optional} containing the declaration of the target
     *         class or interface if found; otherwise, an empty {@code Optional}.
     */
    private Optional<ClassOrInterfaceDeclaration> getClassDeclaration(CompilationUnit cu, String className) {
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration classOrInterface : classes) {
            if (classOrInterface.getNameAsString().equals(className)) {
                LOGGER.debug("Retrieved the targeted class declaration: {}", className);
                return Optional.of(classOrInterface);
            }
        }

        LOGGER.warn("The targeted class declaration was not found in the provided compilation unit.");
        return Optional.empty();
    }

    /**
     * Creates a new {@link MethodDeclaration} object from the provided method signature.
     *
     * @param signature     the signature of the method to be created
     * @param parser        the Java code parser
     * @param newMethodCode the new method code
     * @return the newly constructed {@link MethodDeclaration} object
     */
    private MethodDeclaration createNewMethodFromSignature(JavaCodeParser.MethodSignature signature, JavaCodeParser parser, String newMethodCode) {
        LOGGER.info("Creating a new method from the provided signature.");

        MethodDeclaration newMethod = new MethodDeclaration();

        if (signature.modifierPresent() != JavaCodeParser.ModifierPresent.ABSENT) {
            signature.modifierKeyword().forEach(newMethod::addModifier);
        }

        newMethod.setType(signature.returnType());
        newMethod.setName(signature.methodName());

        LOGGER.debug("Set method name to '{}' and return type to '{}'.", signature.methodName(), signature.returnType());

        List<String> rawParameters = splitParameters(signature.parameters());
        boolean isRawParamsEmpty = rawParameters.stream().anyMatch(param -> param.trim().isEmpty());

        // If the method signature has parameters,
        // parse them and add them to the new method.
        // Otherwise, skip and set the method body
        if (!isRawParamsEmpty) {
            NodeList<Parameter> parameters = new NodeList<>();

            for (String rawParam : rawParameters) {
                String[] parts = rawParam.trim().split(" ");

                if (parts.length >= 2) {
                    List<String> typeParts = Arrays.stream(Arrays.copyOf(parts, parts.length - 1))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    String typeName = String.join(" ", typeParts);
                    String paramName = parts[parts.length - 1];

                    Parameter parameter = new Parameter(StaticJavaParser.parseType(typeName), paramName);
                    parameters.add(parameter);
                    LOGGER.debug("Added parameter of type '{}' with name '{}'.", typeName, paramName);
                } else {
                    LOGGER.error("Invalid parameter format encountered: '{}'. Throwing exception.", rawParam);
                    throw new IllegalArgumentException("Invalid parameter: " + rawParam);
                }
            }
            newMethod.setParameters(parameters);
            LOGGER.debug("All parameters set for the method.");
        } else {
            LOGGER.debug("No parameters provided for the method.");
        }

        newMethod.setBody(StaticJavaParser.parseBlock(parser.extractMethodBody(newMethodCode)));
        LOGGER.debug("Set method body.");

        return newMethod;
    }

    /**
     * Writes the updated compilation unit back to the file.
     *
     * @param path the path to the Java file
     * @param cu   the updated compilation unit
     * @return {@code true} if the write operation was successful; {@code false} otherwise.
     */
    private boolean writeCompilationUnitToFile(Path path, CompilationUnit cu) {
        try {
            LOGGER.debug("Writing updated compilation unit to file...");
            Files.write(path, cu.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException ex) {
            String errorMessage = (ex.getMessage() != null) ? ex.getMessage() : "Unknown error";
            LOGGER.error("Error writing to file {}: {}", path, errorMessage);
            return false;
        }
    }

    /**
     * Replaces the original method in the target file with a checked (modified) method.
     *
     * @param checkedFile The path to the Java file containing the checked (Checker Framework compiled) method.
     * @param targetFile  The path to the Java file containing the original method to be replaced.
     * @return true if the method replacement was successful; false otherwise.
     * @throws FileNotFoundException If any of the files are not found.
     */
    public boolean replaceOriginalTargetMethod(String checkedFile, String targetFile, String methodName) throws FileNotFoundException {
        JavaCodeParser extractor = new JavaCodeParser();

        ClassOrInterfaceDeclaration checkedClass = extractor.extractClassByMethodName(checkedFile, methodName);

        boolean wasOriginalMethodReplaced = replaceMethodInFile(targetFile, checkedClass.getNameAsString(), checkedClass.toString());
        if (!wasOriginalMethodReplaced) {
            LOGGER.error("Failed to replace the original method in the target file.");
            throw new RuntimeException("Failed to replace original method in file.");
        }

        LOGGER.info("Original method in the target file replaced successfully.");
        return true;
    }
}

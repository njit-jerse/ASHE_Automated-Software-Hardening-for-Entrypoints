![ASHE Logo](https://i.ibb.co/415pDMQ/ASHE-Logo-Dark-BG.png#gh-dark-mode-only)
![ASHE Logo](https://i.ibb.co/YZdDbzX/ASHE-Logo-Light-BG.png#gh-light-mode-only)

# Automated Software Hardening for Entrypoints

ASHE aims to automatically enhance software robustness against unforeseen or harmful inputs. By focusing on entrypoint 
methods—those callable from outside the program—ASHE checks for potential undesired behaviors, such as array overflows 
or null-pointer dereferences. When such vulnerabilities are detected, ASHE auto-synthesizes patches to harden the 
software. Each patch undergoes a stringent verification process, ensuring the enhanced software stands robust against 
malicious or unexpected inputs.

## Overview

- **[Specimin](https://github.com/kelloggm/specimin)**: A tool that, given a Java program P and a set of methods M, 
produces a minimized, compilable version of P preserving the signatures of methods, structure of classes, and other 
specifications used in methods within M.
- **[Checker Framework](https://checkerframework.org/)**: A compiler-integrated tool that checks for specific types of 
errors in the Java code.
- **LLM (e.g., [ChatGPT](https://platform.openai.com/docs/introduction))**: Utilized to generate the expected 
appropriate code patches based on the warnings and errors identified by the Checker Framework.

<br/>

---

## Workflow

1. Use Specimin to minimize the targeted Java code in a temporary directory, focusing on methods that require 
verification.
2. Compile and check the minimized code using the Checker Framework.
3. Any errors found during compilation are prompted to the LLM to generate a patch. If no errors are found, skip
to step 7.
4. The patch from the LLM response is then applied to the minimized code within the temporary directory.
5. Recompile the modified code using the Checker Framework. If additional errors are identified after compilation, 
repeat steps 3-5 until no further errors are found.
6. Replace the original code that was minimized from the absolute path with the modified code in the temporary 
directory.
7. If there were no original errors, or the original code was successfully overwritten, exit the program.

<br/>

---

## Usage

> 🔴 **Important Note:**
> Several updates are in progress to streamline the setup and usage of ASHE. Always refer to the latest version of 
this README for up-to-date instructions.

> 🔴 **Upcoming changes:**
> - We aim to integrate the maven repository version for Specimin, eliminating the need for a specific directory.
> - Plans are in place to remove the dependencies on resources and libs directories in the compilation and execution 
commands by moving them to the gradle build file.

### Compiling ASHE

1. **Download the project:**
    - Obtain the ASHE project files and download them to your local machine.
2. **Navigate to Project Directory:**
    - Open your terminal and navigate to the project root directory using the `cd` command.
3. **Compile ASHE:**
    - Execute the following command:
      ```bash
      javac -cp ".:../resources:../../../libs/*" edu/njit/jerse/*.java
      ```

### Running ASHE

1. **Execute ASHE:**
    - After successful compilation, run ASHE using the following command:
      ```bash
      java -cp ".:../../../libs/*:../resources" edu.njit.jerse.ASHE "/root/path/to/your/targeted/project" "path/to/targetFile/Example.java" "com.example.Example#foo()"
      ```

**Command Arguments:**
- **--root:** Root directory of the target project.
    - Example: `/root/path/to/your/targeted/project`
- **--targetFile:** Source file to search for target methods.
    - Example: `path/to/targetFile/Example.java`
- **--targetMethod:** Target method to preserve. Format: 
`class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)`.
    - Example: `com.example.Example#foo()`

<br/>

---

## Limitations

- While ASHE utilizes Specimin to minimize the targeted Java code, Specimin is still in its early stages and may not 
fully function for complex projects.
- The system's focus is currently on Java, which limits its applicability to other languages.
- There is potential for the LLM to generate patches that do not fully address the identified errors or cause 
additional errors.
- ASHE is currently limited to only utilizing ChatGPT as the LLM. While ChatGPT has shown promising results, the intent 
is to expand the LLM options to include other models - allowing users to select the model that best fits their needs.
- The user will need to create their own OpenAI API key to utilize ChatGPT.
  > 🔴 **Important Note:** There will be a generic configuration file provided in the repository as an example of how 
to set up the ChatGPT API and the prompt settings for the LLM.

## Contributing

If you have suggestions, bug reports, or contributions to the project, please open an issue or submit a pull request 
and the ASHE team will attempt to respond in a timely manner.

## License

This project is licensed under the MIT License. Refer to the LICENSE file for more details.

## Contact

For more information, please reach out to Jonathan Phillips at [jmp274@njit.edu](mailto:jmp274@njit.edu).
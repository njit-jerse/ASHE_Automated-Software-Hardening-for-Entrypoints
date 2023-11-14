![ASHE Logo](https://i.ibb.co/415pDMQ/ASHE-Logo-Dark-BG.png#gh-dark-mode-only)
![ASHE Logo](https://i.ibb.co/YZdDbzX/ASHE-Logo-Light-BG.png#gh-light-mode-only)

# Automated Software Hardening for Entrypoints

ASHE aims to automatically enhance software robustness against unforeseen or harmful inputs. By focusing on entrypoint 
methods—those callable from outside the program—ASHE checks for potential undesired behaviors, such as array overflows 
or null-pointer dereferences. When ASHE detects such a vulnerability, it auto-synthesizes patches to harden the 
software.

ASHE first employs a minimization tool to reduce a program, ensuring it only encompasses a 
single method and that method's dependencies. Subsequently, it utilizes a verification tool, like a pluggable 
typechecker, to pinpoint code that may be susceptible to unexpected inputs. Concluding this process, a large language 
model (LLM) is used to rewrite the code, ensuring its compliance with the verification tool. Through this 
procedure, ASHE generates provably-hardened code.

## Overview

ASHE has three main components: a verification minimizer, a set of verification tools, and a patch generator.  
In our prototype, those three tools are:

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

> 🔴 **Important Note:** ASHE is still under active development, so expect frequent changes to its setup and usage. 
If you'd like to use ASHE, we suggest contacting us first.

### Initial Setup

#### Download ASHE
- Download [ASHE](https://github.com/jonathan-m-phillips/ASHE_Automated-Software-Hardening-for-Entrypoints) from 
our GitHub to your local machine.

#### Creating a Configuration File
- **Using `example.properties` as a Template:**
    - Rename the `example.properties` file in the `resources` directory to `config.properties`.
    - > 🔴 **Important Note:** Ensure this file is ignored by git to prevent any sensitive information from being 
    committed. The `config.properties` file will contain your OpenAI API key. If any sensitive information is 
    committed, immediately revoke your API key and generate a new one.

#### Downloading Specimin
- Navigate to [Specimin](https://github.com/kelloggm/specimin) on GitHub and download the project to your local
machine.
- Add the absolute path you downloaded Specimin to, to the `config.properties` file in the `resources` directory, 
replacing the `specimin.tool.path` placeholder.

#### Creating the ChatGPT API Key
1. **Create an OpenAI account:**
    - Create an account on the [OpenAI](https://platform.openai.com) website.
2. **Create an OpenAI API key:** 
    - Create an [API](https://platform.openai.com/account/api-keys) key for the ChatGPT API under View API Keys.
3. **Add the API key to `config.properties`:** 
    - Copy the API key from OpenAI and paste it into the `config.properties` file in the `resources` directory,
   replacing the `llm.api.key` placeholder.

### Compiling ASHE

1. **Navigate to project directory:**
    - Open your terminal and navigate to the project root directory using the `cd` command.
2. **Compile ASHE:**
    - Execute the following command:
      ```bash
      javac -cp ".:../resources:../../../libs/*" edu/njit/jerse/*.java
      ```

### Running ASHE

1. **Execute ASHE:**
    - After successful compilation, run ASHE using the following command:
      ```bash
      java -cp ".:../../../libs/*:../resources" edu.njit.jerse.ashe.ASHE "/root/path/to/your/targeted/project" "path/to/targetFile/Example.java" "com.example.Example#foo()"
      ```

**Command Arguments:**
- **--root:** Root directory of the target project.
    - Example: `/root/path/to/your/targeted/project`
- **--targetFile:** Source file to search for target methods.
    - Example: `path/to/targetFile/Example.java`
- **--targetMethod:** Target method to preserve. Format: 
`class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)`.
    - Example: `com.example.Example#foo()`

### Logging
> 🟡 **Note:** All logs are written to the `logs` directory in the project root directory.

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
- The user will need to create their own OpenAI API key to utilize ChatGPT, as stated in [Usage](#usage).

## Contributing

If you have suggestions, bug reports, or contributions to the project, please open an issue or submit a pull request.

## License

This project is licensed under the MIT License. Refer to the LICENSE file for more details.

<br>
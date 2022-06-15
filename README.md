# Compilers Project

## GROUP 4C

- António Ribeiro, up201906761, 16, 28% 
- Mário Travassos, up201905871, 16, 28%
- Ricardo Pinto, up201806849, 15, 16%
- Tiago Rodrigues, up201907021, 16, 28% 


GLOBAL Grade of the project: 16

## Project description

### SUMMARY:

As described by the project's guidelines, the tool developed in this class is a compiler of jmm (Java--) code files.
It implements the several stages of compilation, Parsing, Syntax and Semantic analysis, Intermediate code generation
and assembly type instructions (in this case jasmin), converting the original jmm to a executable class file.

### SEMANTIC ANALYSIS:

#### Type Verification

- Verify if variable names used in the code have a corresponding declaration, either as a local variable, a method
  parameter or a field of the class.

- Operations must be between elements of the same type, and operands of an operation must be of types compatible with
  the operation (e.g. int + boolean is an error because +
  expects two integers.)

- Array cannot be used in arithmetic operations (e.g. array1 + array2 is an error)

- Array access is done over an array (e.g 1[10] is not allowed)

- Array access index and is an expression of type integer (e.g a[true] is not allowed)

- Array creation is done with integer values

- Type of the assignee must be compatible with the assigned (an_int = a_boolean is an error)

- Expressions in conditions must return a boolean (if(2+3) is an error)

- Check if fields are not being called from a static context (main method)

#### Function Verification

- When calling methods of the class declared in the code, verify if the types of arguments of the call are
  compatible with the types in the method declaration, and there is the correct number of arguments

- Check if non-static method are being called from main (the only static function)

- In case the method does not exist, verify if the class extends another class and report an error if it does not.
  Assume the method exists in one of the super classes, and that is being correctly called

- When calling methods that belong to other classes other than the class declared in the code, verify if the
  classes are being imported

- Check that the return type of a function is the same as the one in the method declaration

### CODE GENERATION:

#### Ollir

- After the Semantic analysis step, we gather the information given by the symbol table to parse the jmm and construct
  our code.
- This stage is delegated to 5 different visitor classes (modular approach): ArgumentOllirGenerator (for method calls
  arguments), DotLinkedOllirGenerator (for chained expressions, such as length or invocations), ExprOllirGenerator (for
  a broad range of expressions), MethodBodyOllirGenerator (for method bodies parsing) and OllirGenerator (the parent of
  all, that starts the ollir visit).
- Pros: Covers a wide range of scenarios, from basic cases to more specific instructions (given the recursive visits).
- Cons: Since the tree structure, in some cases is too dependent on the operation/instruction itself, it's easy to lose
  context of the expression where the node is embedded in, specially when having to move it to a different visitor
  class. The modular approach also requires some code redundancy.

#### Jasmin

- The jasmin code generation is made through the OllirToJasmin class. It utilizes the ClassUtils object (provided by the OllirResult, from the previous step), that gathers information from each ollir instruction, that are then translated into the Jasmin, according to their context and type. 
- Pros: Each individual scenario/instruction handling is isolated/delegated into one single method, responsible for the code creation (following the "visit" type approach). Each of these code generation methods are designed to be modular, in the sense that they are as independent as possible from eachother, and can thereby be chained and used as building blocks in order to compile more complex instructions.
- Cons: placing the code generation in one file, instead of a more modular and package-oriented approach may lead to refactoring slow downs, in the eventuality of the the language standard changing.

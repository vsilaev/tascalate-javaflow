# Continuation support for Java
This project contains libary, tools and examples to develop Java applications using continuations. 

According to Wikipedia "a continuation is an abstract representation of the control state of a computer program; a continuation reifies the program control state, i.e. the continuation is a data structure that represents the computational process at a given point in the process's execution; the created data structure can be accessed by the programming language, instead of being hidden in the runtime environment"

In other words, continuation allows you to capture execution state of the program (local variables, execution stack, program counters etc.) at the certain places and later resume execution from the saved point. Unfortunately, Java has no support for first-class continuations, but it can be added via bytecode instrumentation (like this library did)

The project is based on the completely reworked Apache Jakarta-Commons JavaFlow project (http://commons.apache.org/sandbox/commons-javaflow/). Below is a list of major changes:

1. Original JavaFlow instruments bytecode of each and every method available for tooling to add Continuations support. This adds significant overhead to both code size and execution time. The reworked version requires developer to mark Continuation-aware classes/methods explicitly with annotations.
2. Code is updated to use new bytecode instrumentation tools (ObjectWeb ASM 5.x); support for BCEL is discontinued 
3. Codebase is split to separate modules: run-time API, instrumentation provider SPI, offline instrumentation tools (Maven [TBD], Ant[TBD]), run-time instrumentation JavaAgent, utilities and examples
4. The library adds support for recent Java features like lambdas and dynamic dispatch

# Maven

[In progress]

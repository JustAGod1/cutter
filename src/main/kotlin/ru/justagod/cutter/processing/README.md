Here the magic happens.

You may need to read [Mincer](../mincer/Mincer.kt) documentation first to understand what is going on here.  

So cutter made its work in two steps:
1. Analyzing
2. Modifying with validation

Both steps behave differently depending on config passed.

When analyzing primal target is to fill up [ProjectModel](model/ProjectModel.kt) with atoms. 

When modifying we use gathered information from previous step to make decisions about fate of 
each element of the given bytecode.

Via the same information we also validate if there is no invalid method calls or field invocations. 
It's called validation.

More information you can find in [ProjectModel](model/ProjectModel.kt).


```
.  
├── analization - first step
├── base - just handy classes that shared between steps
├── config - configuration of cutter
├── model - classes that help to build project in memory model
└── transformation - second step
```

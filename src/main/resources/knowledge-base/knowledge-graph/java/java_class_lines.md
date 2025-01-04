## Overview
A block which represents java Class. Only classes that are declared as 'public class {class_name}' in provided file should be extracted. 
If there is only one declared class in a file then return array with single item 

### name
Should always be a string 'java_class'

### id
Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`

## Fields

### package
Example - `dev.omyshko.contentmanagement.core.service`  

### class_name
Example - GitContentManager  

#### fromLine
Line number Starting from package declaration including

#### toLine
Line number Where the class ends with a closing brace  


## Dependencies

### imports
Should contain a single item denoting from_line and to_line of imports block

#### connection_type
Always `configures` 

#### id
Fully qualified class name + '_imports'
Example: `dev.omyshko.contentmanagement.core.service.GitContentManager_imports`

#### dependency_type
Should always be a string `java_class_import`

#### from_line

#### to_line
Including



### constructors
Any methods like Spring @PostConstruct that is supposed to initialize this class

#### connection_type
Always `configures`

#### id
A fully qualified method signature.
Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.GitContentManager(java.lang.String,int)`
Do not add `throws` declaration to signature

#### dependency_type
Should always be a string `java_class_constructor`

#### from_line
Where constructor starts

#### to_line
Line number of closing brace for constructor


### declare_methods
If this class declares any method

#### connection_type
Always `contains`

#### dependency_type
Should always be a string `java_method`

#### from_line
Annotations should be included!

#### to_line
A line number where method closing brace `}` is located.

#### method_closing_brace_line_number
A line number where method terminal brace in located

For example: 
`
145:             errors.stream().map(IllegalStateException::new).forEach(ne::addSuppressed);
146:             throw ne;
147:         }
148:     }
`

A line 148 should be returned


#### id
A fully qualified method signature WITHOUT throws declaration. 
Parameters should be fully qualified and without modifiers like `final` 
Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.manageContent(java.lang.String,int)`

When constructing the fully qualified method signature, you combine:
1. The fully qualified name of the class (or interface) it belongs to.
2. The method signature (method name + parameter types)
   Method Signature with Inner Classes Example: `dev.omyshko.contentmanagement.core.service.GitContentManager$InnerClass.innerMethod(java.lang.String)`


### inner_declarations
List all inner `record`, `interface`, `class`.

#### dependency_type
Should always be a string `java_inner_class`

#### id
`$` separated
Value is created by pattern {outer_class}`$`{inner_class}
Fully qualified inner class name according to java standards
1. the name of the outer class and 
2. `$`
3. the name of the inner class 


Examples: `dev.example.TestEndpoint$BlocksExtractor`, `com.example.GitContentManager$InnerClass$NestedInnerClass`

#### from_line

#### to_line
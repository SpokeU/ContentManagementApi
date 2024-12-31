## Overview
A block which represents java Class. Only classes that are declared as 'public class {class_name}' in provided file should be extracted. If there is only one declared class in a file then return array with single item 

### name
Should always be a string 'java_class'

### id
Fully qualified class name. For example `dev.omyshko.contentmanagement.core.service.GitContentManager`

## Fields

### package
Example - `dev.omyshko.contentmanagement.core.service`  

### class_name
Example - GitContentManager  


## Dependencies

### imports
Should contain a single item denoting fromLine and toLine of imports block

#### dependency_type
Should always be a string `java_class_import`

#### fromLine

#### toLine

### declares_method
If this class declares a method. List all declared methods here

#### dependency_type
Should always be a string `java_method`

#### id
A fully qualified method signature.
When constructing the fully qualified method signature, you combine:
1. The fully qualified name of the class (or interface) it belongs to.
2. The method signature (method name + parameter types)
Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.manageContent(java.lang.String,int)`
Method Signature with Inner Classes Example: `dev.omyshko.contentmanagement.core.service.GitContentManager$InnerClass.innerMethod(java.lang.String)`

#### fromLine
If a javadoc or annotation are included it should be captured to fromLine as well

#### toLine

### inner_declarations
List all inner `record`, `interface`, `class`. 
Value is create by pattern {outer_class}`$`{inner_class}

#### dependency_type
Should always be a string `java_inner_class`

#### id
Fully qualified inner class name according to java standards
1. the name of the outer class and 
2. `$`
3. the name of the inner class 


Examples: `dev.example.TestEndpoint$BlocksExtractor`, `com.example.GitContentManager$InnerClass$NestedInnerClass`

#### fromLine

#### toLine
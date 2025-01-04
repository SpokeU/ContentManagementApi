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
Should contain a single item denoting from_line and to_line of imports block

#### id
Fully qualified class name + '_imports'
Example: `dev.omyshko.contentmanagement.core.service.GitContentManager_imports`

#### dependency_type
Should always be a string `java_class_import`

#### from_line

#### to_line
Including

### declares_method
If this class declares a method. List all declared methods here

#### connection_type
If you are declaring dependency then you need to specify what king of connection this is.
It has to be one of the System specified as it helps system to figure out how collect the data when passing to LLM
For example:
java_class-:contains->java_method
java_class-:->java_method

This can be implemented either as connection_type field which will be a property of connection. 
Or replace declares_method to actual connection_type and use separate field to have

**requires** або **чи_Впливає_на_роботу** - Такий тип звязку каже чи обовязково мені ця залежність шоб себе описати?
Наприклад якщо я заміню Import для цього блоку (java_class) це повпливає на все що всередині нього?


#### id
A fully qualified method signature.
Example: `dev.omyshko.contentmanagement.core.service.GitContentManager.manageContent(java.lang.String,int)`
Do not add `throws` declaration to signature

When constructing the fully qualified method signature, you combine:
1. The fully qualified name of the class (or interface) it belongs to.
2. The method signature (method name + parameter types)
   Method Signature with Inner Classes Example: `dev.omyshko.contentmanagement.core.service.GitContentManager$InnerClass.innerMethod(java.lang.String)`

#### dependency_entity_type
Should always be a string `java_method`

#### from_line
If a javadoc or annotation are included it should be captured to from_line as well

#### to_line

### inner_declarations
List all inner `record`, `interface`, `class`.

#### dependency_block_type
What that dependency entity type? java_method, java_class, html_template? etc..
Should always be a string `java_inner_class`

#### id
`$` separated
Value is created by pattern {outer_class}`$`{inner_class}
Fully qualified inner class name according to java standards
1. the name of the outer class and
2. `$`
3. the name of the inner class


Examples: `dev.example.TestEndpoint$BlocksExtractor`, `com.example.GitContentManager$InnerClass$NestedInnerClass`

#### name
class name

#### from_line

#### to_line
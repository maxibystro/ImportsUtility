# ImportsUtility
ImportsUtility replaces local imports with framework imports (via angle brackets) in Objective-C header files.

### SYNOPSIS
`groovy ImportsUtility.groovy [ options ]  [ sourceheaders ]`  
#### sourceheaders  
One or more source files to be edited (such as MyClass.h and MyClass.m).

### OPTIONS
#### -uh
Specify where to find frameworks umbrella headers. Character ':' is used as path separator.
#### -dfn
Specify default framework name. This name is used for headers that were not found in umbella headers.

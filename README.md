# ImportsUtility
ImportsUtility replaces local imports with framework imports (via angle brackets) in Objective-C header files.

### SYNOPSIS
`groovy ImportsUtility.groovy [ options ]  [ sources ]`  
#### sources  
One or more source files to be edited (such as MyClass.h and MyClass.m). Directories are also applicable.

### OPTIONS
#### -uh
Specify where to find frameworks umbrella headers. Character ':' is used as path separator.
#### -dfn
Specify default framework name. This name is used for headers that were not found in umbella headers.
#### -fuh
Fix j2objc frameworks umbrella headers. It means that framework name will be added to each import.

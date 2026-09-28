# Bavarian-C / TCLang Specification

**Status**: Authoritative Ground Truth Reference  
**Target Architecture**: TheComputer 64-bit Architecture (`WORD_BITS=64`, `ADDRESS_BITS=32`, 32 GPRs)  
**File Extension**: `.tc` / `.c`  
**Version**: 1.0.0  

---

## 1. Overview & Design Goals

**Bavarian-C** (`TCLang`) is a minimal, strongly-typed, imperative systems programming language designed to compile directly to **TheComputer 64-bit Instruction Set Architecture**. It provides low-level memory access, pointer arithmetic, structures, and C-compatible calling conventions while retaining a simple, easily parseable grammar.

### Key Characteristics:
* **Native 64-Bit Data Path**: Registers and pointers are 64-bit (`WORD_BITS = 64`).
* **Byte-Addressable 32-Bit Memory Space**: Pointers hold 32-bit effective virtual addresses zero-extended to 64-bit words.
* **C-Style Semantics**: Explicit types, pointer dereferencing, struct member access, and stack-frame management.
* **Predictable Assembly Output**: 1:1 or near-1:1 lowering from 3-Address IR to 64-bit machine instructions.

---

## 2. Lexical Structure

### 2.1 Character Set & Whitespace
* Source code is UTF-8 text.
* Whitespace (spaces, tabs, carriage returns, newlines) acts as token delimiters and is otherwise ignored outside string/character literals.

### 2.2 Comments
* **Single-Line**: `// comment until end of line`
* **Multi-Line**: `/* block comment (cannot nest) */`

### 2.3 Identifiers
* Match regex: `[a-zA-Z_][a-zA-Z0-9_]*`
* Case-sensitive (`value` != `Value`).

### 2.4 Keywords
```c
void      byte      char      short     int       long      struct    sizeof
if        else      while     for       return    break     continue
```

### 2.5 Literals
* **Integer Dec**: `0`, `42`, `1000000`
* **Integer Hex**: `0x0`, `0x1F`, `0xFFFFFFFF`
* **Integer Binary**: `0b1010`
* **Character**: `'a'`, `'\n'`, `'\0'`, `'\\'`, '\'' (8-bit ASCII value)
* **String**: `"Hello, World!\n"` (Null-terminated ASCII byte array in `.rodata`)

### 2.6 Operators & Delimiters
* **Arithmetic**: `+`, `-`, `*`, `/`, `%`
* **Bitwise**: `&`, `|`, `^`, `~`, `<<`, `>>`
* **Logical**: `&&`, `||`, `!`
* **Comparison**: `==`, `!=`, `<`, `<=`, `>`, `>=`
* **Assignment**: `=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`, `<<=`, `>>=`
* **Pointer & Member**: `*`, `&`, `.`, `->`, `[`, `]`
* **Delimiters**: `(`, `)`, `{`, `}`, `;`, `,`

---

## 3. Type System & Memory Layout

All values occupy either 64-bit registers during evaluation or aligned bytes in memory.

| Type | Width (Bytes) | Alignment | Value Range | Register Representation |
| :--- | :---: | :---: | :--- | :--- |
| **`void`** | 0 | - | Empty | None |
| **`byte` / `char`** | 1 | 1 | `-128` .. `127` (or `0`..`255`) | Low 8 bits, sign/zero extended |
| **`short`** | 2 | 2 | `-32,768` .. `32,767` | Low 16 bits, sign/zero extended |
| **`int`** | 4 | 4 | `-2,147,483,648` .. `2,147,483,647` | Low 32 bits, sign extended |
| **`long`** | 8 | 8 | `-2^63` .. `2^63-1` | Full 64-bit GPR |
| **`T*` (Pointer)** | 8 | 8 | `0x00000000` .. `0xFFFFFFFF` | Full 64-bit GPR (32-bit active address) |
| **`T[N]` (Array)** | `N * sizeof(T)` | `align(T)` | Contiguous elements | Decays to pointer `T*` in expressions |
| **`struct S`** | Sum of fields + pad | Max field align | Aggregated fields | Base address in memory/stack |

### Pointer Arithmetic Rule
When adding/subtracting an integer `n` to a pointer `p` of type `T*`:
`effective_address(p + n) = address(p) + (n * sizeof(T))`

---

## 4. Operator Precedence & Associativity

Ordered from highest priority (1) to lowest priority (14):

| Level | Operator | Description | Associativity |
| :---: | :--- | :--- | :---: |
| **1** | `()`, `[]`, `.`, `->`, `postfix ++`, `postfix --` | Primary & Postfix | Left-to-Right |
| **2** | `!`, `~`, `+`, `-` (unary), `*` (deref), `&` (addr-of), `sizeof`, `prefix ++`, `prefix --` | Unary | Right-to-Left |
| **3** | `*`, `/`, `%` | Multiplicative | Left-to-Right |
| **4** | `+`, `-` | Additive | Left-to-Right |
| **5** | `<<`, `>>` | Bitwise Shifts | Left-to-Right |
| **6** | `<`, `<=`, `>`, `>=` | Relational Comparisons | Left-to-Right |
| **7** | `==`, `!=` | Equality Comparisons | Left-to-Right |
| **8** | `&` | Bitwise AND | Left-to-Right |
| **9** | `^` | Bitwise XOR | Left-to-Right |
| **10** | `\|` | Bitwise OR | Left-to-Right |
| **11** | `&&` | Logical AND (Short-Circuit) | Left-to-Right |
| **12** | `\|\|` | Logical OR (Short-Circuit) | Left-to-Right |
| **13** | `?:` | Conditional Ternary | Right-to-Left |
| **14** | `=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `\|=`, `^=`, `<<=`, `>>=` | Assignment | Right-to-Left |

---

## 5. Formal EBNF Grammar

```ebnf
Program          ::= { TopLevelDecl } ;

TopLevelDecl     ::= StructDecl | FunctionDecl | GlobalVarDecl ;

Type             ::= BaseType { "*" } ;
BaseType         ::= "void" | "byte" | "char" | "short" | "int" | "long" | "struct" Ident | Ident ;

StructDecl       ::= "struct" Ident "{" { MemberDecl } "}" ";" ;
MemberDecl       ::= Type Ident [ "[" IntegerLit "]" ] ";" ;

GlobalVarDecl    ::= Type Ident [ "[" IntegerLit "]" ] [ "=" Initializer ] ";" ;
Initializer      ::= Expr | "{" Expr { "," Expr } "}" | StringLit ;

FunctionDecl     ::= Type Ident "(" [ ParamList ] ")" ( BlockStmt | ";" ) ;
ParamList        ::= Param { "," Param } ;
Param            ::= Type Ident [ "[" "]" ] ;

BlockStmt        ::= "{" { Stmt } "}" ;

Stmt             ::= LocalVarDecl
                   | BlockStmt
                   | IfStmt
                   | WhileStmt
                   | ForStmt
                   | ReturnStmt
                   | BreakStmt
                   | ContinueStmt
                   | ExprStmt ;

LocalVarDecl     ::= Type Ident [ "[" IntegerLit "]" ] [ "=" Expr ] ";" ;

IfStmt           ::= "if" "(" Expr ")" Stmt [ "else" Stmt ] ;
WhileStmt        ::= "while" "(" Expr ")" Stmt ;
ForStmt          ::= "for" "(" [ Expr | LocalVarDecl ] ";" [ Expr ] ";" [ Expr ] ")" Stmt ;
ReturnStmt       ::= "return" [ Expr ] ";" ;
BreakStmt        ::= "break" ";" ;
ContinueStmt     ::= "continue" ";" ;
ExprStmt         ::= [ Expr ] ";" ;

Expr             ::= Assignment ;
Assignment       ::= Conditional [ ( "=" | "+=" | "-=" | "*=" | "/=" | "%=" | "&=" | "|=" | "^=" | "<<=" | ">>=" ) Assignment ] ;
Conditional      ::= LogicalOr [ "?" Expr ":" Conditional ] ;
LogicalOr        ::= LogicalAnd { "||" LogicalAnd } ;
LogicalAnd       ::= BitOr { "&&" BitOr } ;
BitOr            ::= BitXor { "|" BitXor } ;
BitXor           ::= BitAnd { "^" BitAnd } ;
BitAnd           ::= Equality { "&" Equality } ;
Equality         ::= Relational { ( "==" | "!=" ) Relational } ;
Relational       ::= Shift { ( "<" | "<=" | ">" | ">=" ) Shift } ;
Shift            ::= Additive { ( "<<" | ">>" ) Additive } ;
Additive         ::= Multiplicative { ( "+" | "-" ) Multiplicative } ;
Multiplicative   ::= Cast { ( "*" | "/" | "%" ) Cast } ;
Cast             ::= "(" Type ")" Cast | Unary ;
Unary            ::= ( "+" | "-" | "!" | "~" | "*" | "&" | "sizeof" ) Unary 
                   | "++" Unary | "--" Unary
                   | Postfix ;
Postfix          ::= Primary { "[" Expr "]" 
                             | "(" [ ArgList ] ")" 
                             | "." Ident 
                             | "->" Ident 
                             | "++" 
                             | "--" } ;
ArgList          ::= Expr { "," Expr } ;

Primary          ::= Ident 
                   | IntegerLit 
                   | CharacterLit 
                   | StringLit 
                   | "(" Expr ")" 
                   | "sizeof" "(" Type ")" ;
```

---

## 6. 64-Bit ABI Calling Convention

### 6.1 Register Roles & Allocation
The CPU provides 32 general-purpose 64-bit registers (`R0`–`R31`).

| Register | Name | Role | Saver |
| :--- | :--- | :--- | :--- |
| `R0` | `T` / Scratch | Plain scratch register (not hardwired to zero) | Caller |
| `R1` | `A0` / `RET` | Argument 1 / Return Value (Integer/Pointer) | Caller |
| `R2` | `A1` / `RET2` | Argument 2 / Secondary Return Value | Caller |
| `R3` | `A2` | Argument 3 | Caller |
| `R4` | `A3` | Argument 4 | Caller |
| `R5` | `A4` | Argument 5 | Caller |
| `R6` | `A5` | Argument 6 | Caller |
| `R7`–`R15` | `T0`–`T8` | Temporary Scratch Registers | Caller |
| `R16`–`R28` | `S0`–`S12` | Saved Registers (Must preserve across calls) | Callee |
| `R29` | `S13` | Saved register (the stack pointer is a special register, reachable as `sp` in `mov`/`addi`/`subi`) | Callee |
| `R30` | `FP` | Frame Pointer (Base of current activation record)| Callee |
| `R31` | `LR` / Scratch | Link Register / Scratch | Caller |

### 6.2 Stack Frame Layout
Stack grows downward (from high memory to low memory).

```
+------------------------------------+  <- High Memory (Previous SP)
|   Arguments 7..N (pushed by caller)|
+------------------------------------+
|   Return Address (pushed by CALL)  |  [FP + 8]
+------------------------------------+
|   Saved Frame Pointer (old FP)     |  [FP]      <- Current FP (R30)
+------------------------------------+
|   Saved Callee Registers (S0..Sn)  |  [FP - 8], [FP - 16]...
+------------------------------------+
|   Local Variables & Arrays         |  [FP - locals_offset]
+------------------------------------+
|   Outgoing Spilled Arguments       |
+------------------------------------+  <- Low Memory (Current SP)
```

### 6.3 Function Prologue & Epilogue

#### Standard Prologue:
```assembly
// Function: my_function(int a, int b)
my_function:
    push r30                    // Save caller's frame pointer
    mov  r30, sp                // Set up new frame pointer
    subi sp, sp, 32             // Allocate 32 bytes for locals & scratch
    push r16                    // Save callee-saved register if used
```

#### Standard Epilogue:
```assembly
    pop  r16                    // Restore callee-saved register
    mov  sp, r30                // Deallocate local variables
    pop  r30                    // Restore caller's frame pointer
    ret                         // Return to caller (pops PC)
```

---

## 7. Memory Sections & Lowering

The compiler generates standard assembly directives for the Assembler:

1. **`.text`**: Executable machine instructions.
2. **`.rodata`**: Constant strings and read-only tables.
3. **`.data`**: Initialized global and static variables.
4. **`.bss`**: Zero-initialized global memory reservation.

### Example Lowering: Global Variable & String Literal
```c
// Source:
int counter = 42;
char *msg = "Hello\n";
```
```assembly
.data
counter:
    .word 42, 0                 // 64-bit integer
msg:
    .word .Lstr0, 0             // Pointer to string literal

.rodata
.Lstr0:
    .byte 72, 101, 108, 108, 111, 10, 0   // "Hello\n\0"
```

---

## 8. Standard Library Runtime Interface (`RTE-001`)

The runtime provides basic hardware MMIO and system utility hooks:

```c
// Console Output
void print_char(char c);
void print_int(long n);
void print_str(char *s);

// Dynamic Memory Allocation
void *malloc(long size_bytes);
void free(void *ptr);

// Hardware / Graphics MMIO
void vram_draw_pixel(int x, int y, int color);
void vram_clear(int color);
int  timer_get_ticks(void);
```

---

## 9. Comprehensive Example Programs

### 9.1 Recursive Fibonacci
```c
int fib(int n) {
    if (n <= 1) {
        return n;
    }
    return fib(n - 1) + fib(n - 2);
}

int main() {
    int result = fib(10);
    print_int(result); // Output: 55
    return 0;
}
```

### 9.2 Pointer Arithmetic & Array In-Place Reverse
```c
void reverse(int *arr, int len) {
    int *left = arr;
    int *right = arr + len - 1;
    while (left < right) {
        int temp = *left;
        *left = *right;
        *right = temp;
        left++;
        right--;
    }
}
```

### 9.3 Structures & DOOM-Style 16.16 Fixed-Point Vector Math
```c
struct Vec2 {
    int x; // 16.16 fixed-point (upper 16 = integer, lower 16 = fraction)
    int y;
};

int fixed_mul(int a, int b) {
    long prod = (long)a * (long)b;
    return (int)(prod >> 16);
}

void vec2_scale(struct Vec2 *v, int scale) {
    v->x = fixed_mul(v->x, scale);
    v->y = fixed_mul(v->y, scale);
}

int main() {
    struct Vec2 player_pos;
    player_pos.x = 10 << 16; // 10.0
    player_pos.y = 20 << 16; // 20.0
    
    vec2_scale(&player_pos, 2 << 16); // Scale by 2.0
    
    print_int(player_pos.x >> 16); // 20
    print_int(player_pos.y >> 16); // 40
    return 0;
}
```

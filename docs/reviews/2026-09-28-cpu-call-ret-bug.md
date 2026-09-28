# Review writeup: `CALL`/`RET` were non-functional — now fixed and verified

**Author:** Claude (Team Lead / Architectural Reviewer role, per `CLAUDE.md`)
**Opened:** 2026-09-28
**Fixed:** 2026-09-28, commit `90bf884` ("fix(cpu): fix call and jmpr")
**Severity (as opened):** High — subroutines did not work at all, for any
program, in any combination of `CALL`/`CALLR` + `RET`.
**File affected:** `src/main/java/de/happybavarian07/computer/cpu/Cpu.java`
**Status: FIXED AND VERIFIED.** The Developer applied both fixes proposed
below, matching this writeup's suggested approach exactly (down to the
`branchTargetAddress` field name for Bug 1). Both were independently
re-verified end-to-end after the fix (see
[Verification](#verification-after-the-fix) below) — not just diffed.

This writeup originally backed findings published in
[`../ASSEMBLY_GUIDE.md`](../ASSEMBLY_GUIDE.md) §9 (points 2–3), which
should now be read as historical (they describe the bug as it stood before
`90bf884`); the guide has since been updated to reflect the fix. It's also
relevant to [`../LANGUAGE_SPECIFICATION.md`](../LANGUAGE_SPECIFICATION.md)
§6 ("64-Bit ABI Calling Convention"), whose prologue/epilogue example ends
every function with `ret` — that `ret` now works, so the ABI's `ret`-based
design is no longer blocked at the instruction level (nothing here verifies
the ABI's own correctness beyond that).

The rest of this document is kept as the original bug report, for the
record of what was wrong and why the fix works. See
[Verification](#verification-after-the-fix) at the end for confirmation the
fix actually resolves it.

---

## Bug 1 (fixed) — `CALL` jumped to the wrong address

### Root cause

`Cpu.push(Word value)` reuses the CPU's shared `workingAddress` scratch
field to compute the stack write address, as a side effect:

```java
private void push(Word value) {
    int newSp = specialRegisters.getSP().getAsInt() - Architecture.INSTRUCTION_BYTES;
    if (newSp < Architecture.STACK_LIMIT_ADDRESS) {
        isHalted = true;
        throw new StackOverflowException("Tried to push data past max stack size.");
    }
    specialRegisters.getSP().set(newSp);
    workingAddress.set(specialRegisters.getSP());   // <-- overwrites workingAddress
    systemBus.writeWord(workingAddress, value);
}
```

`workingAddress` is a single `Address` field shared across the whole
fetch/decode/execute cycle. During the "read" stage, `CALL`'s operand
mapping (`IMM32_ONLY`) sets it to the jump target:

```java
case IMM32_ONLY -> {
    workingAddress.set(currentInstruction.immediateAddr());
}
```

But `CALL`'s original execute case read `workingAddress` for the jump
*after* already calling `push()`, which by then had overwritten it with the
stack address:

```java
// as it stood before the fix
case CALL -> {
    scratchReturnAddrWord.set(specialRegisters.getPC().getAsInt() + Architecture.INSTRUCTION_BYTES);
    push(scratchReturnAddrWord);                          // clobbers workingAddress
    specialRegisters.getPC().set(workingAddress);          // reads the clobbered value
    executionResult.pcUpdate = false;
}
```

Net effect: `CALL` always jumped to wherever the stack pointer ended up
after the push, not to the intended subroutine address. `JMP` was
unaffected (no `push()` call intervenes). `CALLR` was unaffected — it reads
its jump target from `regSrc1Value` instead of `workingAddress`, a field
`push()` never touches.

### Applied fix

Exactly the approach proposed here: a dedicated scratch `Address` field,
`branchTargetAddress`, distinct from `workingAddress`, that `CALL` copies
`workingAddress` into *before* calling `push()`, then reads back afterward:

```java
// commit 90bf884
private final Address branchTargetAddress;   // new field, initialized in the constructor
...
case CALL -> {
    branchTargetAddress.set(workingAddress);
    scratchReturnAddrWord.set(specialRegisters.getPC().getAsInt() + Architecture.INSTRUCTION_BYTES);
    push(scratchReturnAddrWord);
    specialRegisters.getPC().set(branchTargetAddress);
    executionResult.pcUpdate = false;
}
```

This is the minimal, conservative fix: it doesn't touch `push()`, `PUSH`, or
`CALLR`, all of which already worked correctly.

---

## Bug 2 (fixed) — `RET` crashed unconditionally

### Root cause

```java
// as it stood before the fix
case RET -> {
    pop(scratchReturnAddrWord);
    specialRegisters.getPC().set(scratchReturnAddrWord);   // <-- width mismatch
    executionResult.pcUpdate = false;
}
```

`scratchReturnAddrWord` is a `Word` (64 bits). `specialRegisters.getPC()` is
an `Address` (32 bits). `FixedWidthBits.set(FixedWidthBits other)` asserts
`other.size == this.size` and throws otherwise — so this line threw
`IllegalArgumentException: Expected 32 bits, got 64` every single time
`RET` executed, independent of Bug 1 (confirmed at the time by reaching
`RET` via the one call path that *did* jump correctly, `CALLR`, and
observing the same crash).

### Applied fix

Exactly the approach proposed here: narrow the 64-bit value to 32 bits via
`FixedWidthBits.set(Number)` (which truncates by construction, with no size
assertion) instead of `set(FixedWidthBits)` — the same mechanism `JMPR`/
`CALLR` already used:

```java
// commit 90bf884
case RET -> {
    pop(scratchReturnAddrWord);
    specialRegisters.getPC().set(scratchReturnAddrWord.getAsLong() & 0xFFFFFFFFL);
    executionResult.pcUpdate = false;
}
```

---

## Verification after the fix

Re-ran both reproduction programs against commit `90bf884`'s actual
compiled output (not inferred from the diff):

**`CALLR` + `RET`:**
```asm
movi r9, sub
callr r9
halt
sub:
movi r1, 0xBEEF
ret
```
Result: `r1 = 0xBEEF` (48879 decimal) after halt, `SP` restored to its
pre-call value, CPU halts cleanly. Step trace confirms `RET` returns to the
instruction immediately after `callr r9`, not into stack memory or a crash.

**Direct `CALL` + `RET`:**
```asm
call sub
halt
sub:
movi r1, 0xBEEF
ret
```
Result: same — `r1 = 0xBEEF`, `SP` restored, clean halt. Step trace confirms
`CALL` jumps directly to `sub`'s real address (previously it jumped into
stack memory) and `RET` returns to the instruction after `call sub`.

Both bugs are confirmed fixed, independently of each other, exactly as the
proposed fixes predicted. Subroutines (`CALL`/`CALLR` + `RET`) are now safe
to use and document as working. See
[`../ASSEMBLY_GUIDE.md`](../ASSEMBLY_GUIDE.md) §7.4 for a real,
validated example built on this.

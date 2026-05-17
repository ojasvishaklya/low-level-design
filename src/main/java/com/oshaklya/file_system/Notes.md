// ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
// ═══════════════════════════════════════════════════════════════════════════

REQUIREMENTS:
- Thread-safe in-memory file system with hierarchical structure
- Support files and folders (directories)
- Operations: createFile, createFolder, delete, rename, move, find, list
- Path-based addressing with validation
- Fine-grained locking for concurrent access
- Prevent deadlocks during multi-resource operations

Out of Scope:
- Disk persistence (in-memory only)
- File permissions and access control
- File content search
- Symlinks or hard links
- Disk quotas or size limits
- File metadata (timestamps, size, owner)

// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES & RELATIONSHIPS
// ═══════════════════════════════════════════════════════════════════════════

ENTITIES:
- FileSystemEntry: Abstract base for file system nodes (files and folders)
- File: Leaf node containing string content
- Folder: Container node with named children (Map)
- FileSystem: API facade managing root and operations

RELATIONSHIPS:
- FileSystemEntry → FileSystemEntry (parent-child bidirectional)
- Folder HAS-MANY FileSystemEntry (children map: name → entry)
- FileSystem HAS-ONE Folder (root)

// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
// ═══════════════════════════════════════════════════════════════════════════

abstract class FileSystemEntry:
- parent: FileSystemEntry
- name: String
- lock: ReadWriteLock (ReentrantReadWriteLock per entry)

    + isDirectory() -> boolean
    + getLock() -> ReadWriteLock
    + setName(name)
    + getParent() -> FileSystemEntry
    + getPath() -> String                  // Builds full path with proper locking

class File extends FileSystemEntry:
- content: String

    + isDirectory() -> false

class Folder extends FileSystemEntry:
- children: Map<String, FileSystemEntry>

    + isDirectory() -> true
    + addChild(entry)                      // Lock-aware: acquires write lock
    + getChild(name) -> FileSystemEntry    // Lock-aware: acquires read lock
    + hasChild(name) -> boolean            // Lock-aware: acquires read lock
    + removeChild(name) -> FileSystemEntry // Lock-aware: acquires write lock
    + getChildren() -> List<FileSystemEntry>
    + addChildDirectly(entry)              // Internal: assumes caller holds lock
    + getChildDirectly(name)               // Internal: assumes caller holds lock
    + hasChildDirectly(name)               // Internal: assumes caller holds lock
    + removeChildDirectly(name)            // Internal: assumes caller holds lock

class FileSystem:
- root: Folder

    + FileSystem()
    + createFile(path, content) -> String
    + createFolder(path) -> String
    + delete(path) -> String
    + rename(path, newName) -> String
    + move(srcPath, destPath) -> String
    + find(path) -> FileSystemEntry
    + list(path) -> List<String>
    - resolveParentWithLocks(path, lockedEntries) -> Folder
    - resolvePathWithLocks(path, lockedEntries) -> FileSystemEntry
    - extractFileName(path) -> String
    - parsePath(path) -> List<String>
    - compareLockOrder(f1, f2) -> int

// ═══════════════════════════════════════════════════════════════════════════
// KEY ALGORITHMS
// ═══════════════════════════════════════════════════════════════════════════

PATH RESOLUTION (resolveParentWithLocks):
1. Parse path into components [root, dir1, dir2, ..., fileName]
2. Validate path starts with root
3. Acquire read lock on root, add to lockedEntries
4. For each directory component (except last):
   - Check child exists using hasChildDirectly (assumes lock held)
   - Validate is directory
   - Acquire read lock on child, add to lockedEntries
   - Move to next level
5. Return final parent folder with all ancestor locks held
6. Caller responsible for cleanup via lockedEntries list

LOCK UPGRADE PATTERN (createFile, createFolder, delete, rename):
1. resolveParentWithLocks → acquire read locks root-to-parent
2. Release parent's read lock, remove from tracked list
3. Acquire parent's write lock
4. Perform modification (check exists, add/remove child)
5. Release write lock
6. Release all ancestor read locks in reverse order

MOVE OPERATION (deadlock prevention):
1. Lock source parent hierarchy with read locks → upgrade to write
2. Lock source entry with read lock (prevents deletion during move)
3. Lock destination parent hierarchy with read locks
4. Check for circular dependency (if moving folder into its own subtree)
5. Release destination parent read lock
6. DEADLOCK PREVENTION:
   - If srcParent == destParent: simple rename-in-place under single lock
   - Else: Order locks by identityHashCode to ensure consistent acquisition order
   - Acquire first lock, then second lock (both write locks)
   - Perform atomic move: removeChildDirectly + setName + addChildDirectly
7. Release locks in reverse order

CIRCULAR DEPENDENCY CHECK (move folder):
- Walk up from destParent using getParent()
- If any ancestor == source entry → reject move
- Prevents creating cycles in tree structure

// ═══════════════════════════════════════════════════════════════════════════
// CONCURRENCY HANDLING
// ═══════════════════════════════════════════════════════════════════════════

LOCKING STRATEGY:
- Per-node ReadWriteLock (allows multiple readers, exclusive writer)
- Lock ordering: always acquire root → leaf to prevent deadlocks
- Lock upgrade: read → unlock → write (ReentrantReadWriteLock doesn't support upgrade)
- Tracked locks: use lockedEntries list for exception-safe cleanup

RACE CONDITIONS & FIXES:

getPath():
- Race: Parent chain traversal while concurrent rename/move modifies structure
- Fix: Acquire read locks while walking parent chain, release in reverse

resolveParentWithLocks():
- Race: Directory could be deleted/renamed between check and use
- Fix: Hold read locks from root down to target, caller inherits lock chain

createFile / createFolder / delete / rename:
- Race: Check exists + modify are separate (TOCTOU)
- Fix: Upgrade to write lock before check, perform check-then-act atomically

move() with different parents:
- Race: Two threads moving to/from same folders could deadlock (circular wait)
- Fix: compareLockOrder() ensures deterministic lock ordering via identityHashCode
- Always acquire locks in ascending identityHashCode order

move() circular check:
- Race: Source folder structure could change during validation
- Fix: Hold source entry read lock during entire move, prevents deletion/modification

addChild / removeChild / getChild / hasChild:
- Two versions: lock-aware (public API) vs direct (internal, assumes lock held)
- Direct methods used within FileSystem where lock already acquired
- Prevents lock re-entry and improves performance

TYPICAL LOCK SEQUENCES:

Read operation (find, list):
  root.readLock → child1.readLock → child2.readLock → access data → unlock reverse

Write operation (create, delete, rename):
  root.readLock → parent.readLock → unlock parent.readLock → parent.writeLock → modify → unlock

Move operation (same parent):
  root.readLock → parent.readLock → unlock parent.readLock → parent.writeLock → modify → unlock

Move operation (different parents):
  src_root.readLock → src_parent.readLock → unlock src_parent.readLock → src_parent.writeLock
  → src_entry.readLock → dest_root.readLock → dest_parent.readLock
  → unlock dest_parent.readLock → order(src_parent, dest_parent) → first.writeLock
  → second.writeLock → modify → unlock reverse

// ═══════════════════════════════════════════════════════════════════════════
// DESIGN PATTERNS
// ═══════════════════════════════════════════════════════════════════════════

COMPOSITE PATTERN:
- FileSystemEntry as Component (abstract base)
- File as Leaf (no children)
- Folder as Composite (manages children collection)
- Uniform interface: all entries have parent, name, path

FACADE PATTERN:
- FileSystem class provides simple API hiding complexity
- Encapsulates path parsing, validation, locking coordination
- Client doesn't manage locks directly

TEMPLATE METHOD PATTERN:
- FileSystemEntry defines getLock(), getPath() in base
- Subclasses implement isDirectory() behavior
- Common locking logic inherited, type-specific behavior overridden

STRATEGY PATTERN (implicit):
- Direct vs lock-aware methods in Folder
- Same operation, different concurrency assumptions
- FileSystem chooses appropriate strategy based on lock ownership

// ═══════════════════════════════════════════════════════════════════════════
// INTERVIEW QUESTIONS
// ═══════════════════════════════════════════════════════════════════════════

Q: Why can't ReentrantReadWriteLock upgrade from read to write lock directly?
A: Upgrading could cause deadlock if multiple threads hold read locks and both try to upgrade.
   Must release read lock, then acquire write lock. Creates small window of inconsistency,
   but avoided via atomic check-then-act under write lock.

Q: How does compareLockOrder prevent deadlock in move()?
A: Uses identityHashCode to establish total ordering of lock objects. If thread A needs locks
   (X, Y) and thread B needs (Y, X), both will acquire in same order (lower hash first),
   preventing circular wait condition required for deadlock.

Q: Why separate "Direct" methods in Folder?
A: Performance and correctness. Within FileSystem methods, lock already held by caller.
   Re-acquiring would cause deadlock (ReentrantReadWriteLock allows reentrant read locks but
   not read→write). Direct methods document the precondition that lock must be held.

Q: What happens if move() tries to move folder into its own subtree?
A: Validation walks up from destination parent checking if any ancestor equals source entry.
   Source entry locked during entire operation prevents structure changes that could invalidate
   check. Rejects move with error before modification occurs.

Q: Why track locks in ArrayList instead of stack-based try-finally?
A: resolveParentWithLocks has variable depth (path length unknown). Can't use fixed nesting.
   ArrayList allows dynamic tracking + cleanup in single finally block regardless of where
   exception occurred. Unlock in reverse order to match acquisition order (leaf → root).

Q: Can two threads delete the same file concurrently?
A: No. Both acquire write lock on parent folder. First thread gets lock, removes child, releases.
   Second thread gets lock, calls removeChildDirectly, returns null (not found), reports error.
   No corruption, one succeeds, one fails gracefully.

Q: Why does getPath() lock the entire chain instead of just reading parents?
A: Without locks, concurrent rename/move could modify parent references during traversal.
   Could read partially updated chain (torn read) or follow dangling reference. Read locks
   ensure stable snapshot of parent chain for consistent path construction.
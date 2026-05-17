# Thread-Safe File System - Class Diagram

```
┌──────────────────────────────────┐
│     FileSystemEntry              │
│         (abstract)               │
├──────────────────────────────────┤
│ #parent: FileSystemEntry         │
│ #name: String                    │
│ -lock: ReadWriteLock             │
├──────────────────────────────────┤
│ +isDirectory(): boolean          │
│ +getLock(): ReadWriteLock        │
│ +setName(name: String): void     │
│ +getParent(): FileSystemEntry    │
│ +getPath(): String               │
└──────────────────────────────────┘
            △
            │
            │ is-a
            │
      ┌─────┴──────┐
      │            │
      │            │
┌─────┴──────────────────┐          ┌─────────────────────────────────┐
│       File             │          │         Folder                  │
├────────────────────────┤          ├─────────────────────────────────┤
│ -content: String       │          │ -children: Map<String,          │
│                        │          │             FileSystemEntry>    │
├────────────────────────┤          ├─────────────────────────────────┤
│                        │          │ +isDirectory(): boolean         │
└────────────────────────┘          │ +addChild(entry): void          │
                                    │ +getChild(name): FSEntry        │
                                    │ +hasChild(name): boolean        │
                                    │ +removeChild(name): FSEntry     │
                                    │ +getChildren(): List<FSEntry>   │
                                    │ +addChildDirectly(entry): void  │
                                    │ +getChildDirectly(name): FSEntry│
                                    │ +hasChildDirectly(name): boolean│
                                    │ +removeChildDirectly(n): FSEntry│
                                    └─────────────────────────────────┘
                                                  △
                                                  │
                                                  │ aggregation (1:1)
                                                  │
                                    ┌─────────────┴───────────────────┐
                                    │       FileSystem                │
                                    ├─────────────────────────────────┤
                                    │ -root: Folder                   │
                                    ├─────────────────────────────────┤
                                    │ +createFile(path, content): Str │
                                    │ +createFolder(path): String     │
                                    │ +delete(path): String           │
                                    │ +rename(path, newName): String  │
                                    │ +move(src, dest): String        │
                                    │ +find(path): FileSystemEntry    │
                                    │ +list(path): List<String>       │
                                    │ -resolveParent(path, locks): Fol│
                                    │ -resolvePath(path, locks): FSEnt│
                                    │ -extractFileName(path): String  │
                                    │ -parsePath(path): List<String>  │
                                    │ -compareLockOrder(f1, f2): int  │
                                    └─────────────────────────────────┘
```

## Relationships

- **FileSystemEntry → File**: is-a - File extends FileSystemEntry to represent leaf nodes in the file system hierarchy
- **FileSystemEntry → Folder**: is-a - Folder extends FileSystemEntry to represent composite nodes that contain children
- **Folder → FileSystemEntry**: composition (1:many) - Folder owns a map of children entries. When a folder is deleted, all its children are removed from the hierarchy. The parent-child lifecycle is managed through addChild/removeChild operations
- **FileSystem → Folder**: aggregation (1:1) - FileSystem holds a reference to the root folder. The root exists as long as the FileSystem exists, but conceptually the root is independent
- **FileSystemEntry → ReadWriteLock**: composition (1:1) - Each FileSystemEntry owns its own ReadWriteLock instance created during construction. The lock lifecycle is tied to the entry and enables thread-safe concurrent access

## Core Flow

1. FileSystem.createFile(path, content) → resolveParentWithLocks() acquires read locks on path ancestors, upgrades parent to write lock, creates File instance
2. Folder.addChildDirectly(file) → adds file to children map, sets file.parent reference, establishing parent-child relationship
3. FileSystem.move(src, dest) → acquires locks on both source and destination parent folders using compareLockOrder() for deadlock prevention
4. Folder.removeChildDirectly(srcName) → removes entry from source parent's children map, clears parent reference
5. Folder.addChildDirectly(entry) → adds entry to destination parent's children map, sets new parent reference
6. FileSystemEntry.getPath() → traverses parent chain with read locks, building full path string from root to current entry

## Key Decisions

**Composite Pattern**: FileSystemEntry serves as the component interface with File (leaf) and Folder (composite) as concrete implementations, enabling uniform treatment of files and folders.

**Lock Ordering Strategy**: compareLockOrder() uses System.identityHashCode() to establish consistent lock acquisition order between folders, preventing deadlocks during move operations that require multiple write locks.

**Direct vs Locked Methods**: Folder provides both locking methods (addChild, getChild) and direct variants (addChildDirectly) to enable lock upgrading patterns where the caller already holds appropriate locks.

**Path Resolution with Lock Tracking**: resolveParentWithLocks() and resolvePathWithLocks() accumulate locked entries in a list during path traversal, ensuring all locks are released in reverse order via finally blocks for exception safety.

**Parent Reference Management**: The bidirectional parent-child relationship (Folder.children map and FileSystemEntry.parent reference) is maintained consistently during all add/remove operations to support path resolution.

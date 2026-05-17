package com.oshaklya.file_system;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

abstract class FileSystemEntry {
    FileSystemEntry parent;
    String name;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    boolean isDirectory() {
        return false;
    }

    ReadWriteLock getLock() {
        return lock;
    }

    void setName(String name) {
        this.name = name;
    }

    FileSystemEntry getParent() {
        return parent;
    }

    String getPath() {
        List<String> path = new ArrayList<>();
        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        lock.readLock().lock();
        lockedEntries.add(this);

        try {
            FileSystemEntry current = this;

            while (current != null) {
                path.add(current.name);
                FileSystemEntry nextParent = current.parent;

                if (nextParent != null) {
                    nextParent.getLock().readLock().lock();
                    lockedEntries.add(nextParent);
                }

                current = nextParent;
            }

            StringBuilder builder = new StringBuilder();
            for (int i = path.size() - 1; i >= 0; i--) {
                builder.append("/");
                builder.append(path.get(i));
            }

            return builder.toString();
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }
}

class File extends FileSystemEntry {
    String content;
}

class Folder extends FileSystemEntry {
    Map<String, FileSystemEntry> children;

    Folder() {
        this.children = new HashMap<>();
    }

    @Override
    boolean isDirectory() {
        return true;
    }

    void addChild(FileSystemEntry entry) {
        getLock().writeLock().lock();
        try {
            children.put(entry.name, entry);
            entry.parent = this;
        } finally {
            getLock().writeLock().unlock();
        }
    }

    FileSystemEntry getChild(String name) {
        getLock().readLock().lock();
        try {
            return children.get(name);
        } finally {
            getLock().readLock().unlock();
        }
    }

    boolean hasChild(String name) {
        getLock().readLock().lock();
        try {
            return children.containsKey(name);
        } finally {
            getLock().readLock().unlock();
        }
    }

    void addChildDirectly(FileSystemEntry entry) {
        children.put(entry.name, entry);
        entry.parent = this;
    }

    boolean hasChildDirectly(String name) {
        return children.containsKey(name);
    }

    FileSystemEntry getChildDirectly(String name) {
        return children.get(name);
    }

    FileSystemEntry removeChild(String name) {
        getLock().writeLock().lock();
        try {
            FileSystemEntry removed = children.remove(name);
            if (removed != null) {
                removed.parent = null;
            }
            return removed;
        } finally {
            getLock().writeLock().unlock();
        }
    }

    FileSystemEntry removeChildDirectly(String name) {
        FileSystemEntry removed = children.remove(name);
        if (removed != null) {
            removed.parent = null;
        }
        return removed;
    }

    List<FileSystemEntry> getChildren() {
        getLock().readLock().lock();
        try {
            return new ArrayList<>(children.values());
        } finally {
            getLock().readLock().unlock();
        }
    }
}

class FileSystem {
    Folder root;

    FileSystem() {
        root = new Folder();
        root.name = "root";
        root.parent = null;
    }

    String createFile(String path, String content) {
        if (path.equals("/") || path.equals("/" + root.name)) {
            return "Error: Cannot create file at root";
        }

        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        try {
            Folder parent = resolveParentWithLocks(path, lockedEntries);
            String fileName = extractFileName(path);

            parent.getLock().readLock().unlock();
            lockedEntries.remove(lockedEntries.size() - 1);

            parent.getLock().writeLock().lock();
            try {
                if (parent.hasChildDirectly(fileName)) {
                    return "Error: Entry already exists: " + fileName;
                }

                File file = new File();
                file.name = fileName;
                file.content = content;
                parent.addChildDirectly(file);
                return "Created: " + file.getPath();
            } finally {
                parent.getLock().writeLock().unlock();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    private Folder resolveParentWithLocks(String path, List<FileSystemEntry> lockedEntries) throws Exception {
        List<String> pathParts = parsePath(path);

        if (pathParts.size() < 2) {
            throw new Exception("Invalid path - must specify file name");
        }

        if (!pathParts.get(0).equals(root.name)) {
            throw new Exception("Path must start with root: " + root.name);
        }

        root.getLock().readLock().lock();
        lockedEntries.add(root);

        Folder currentFolder = root;
        for (int i = 1; i < pathParts.size() - 1; i++) {
            String folderName = pathParts.get(i);

            if (!currentFolder.hasChildDirectly(folderName)) {
                throw new Exception("Folder not found: " + folderName);
            }

            FileSystemEntry entry = currentFolder.getChildDirectly(folderName);
            if (!entry.isDirectory()) {
                throw new Exception(folderName + " is not a directory");
            }

            entry.getLock().readLock().lock();
            lockedEntries.add(entry);
            currentFolder = (Folder) entry;
        }

        return currentFolder;
    }

    private String extractFileName(String path) throws Exception {
        List<String> pathParts = parsePath(path);

        if (pathParts.isEmpty()) {
            throw new Exception("Invalid path");
        }

        String fileName = pathParts.get(pathParts.size() - 1);
        if (fileName.isEmpty()) {
            throw new Exception("File name cannot be empty");
        }

        return fileName;
    }

    private List<String> parsePath(String path) throws Exception {
        if (path == null || path.isEmpty()) {
            throw new Exception("Path cannot be null or empty");
        }

        String[] pathArray = path.split("/");
        List<String> pathParts = new ArrayList<>();

        for (String part : pathArray) {
            if (!part.isEmpty() && !part.equals(".") && !part.equals("..")) {
                pathParts.add(part);
            }
        }

        return pathParts;
    }

    FileSystemEntry find(String path) {
        List<String> pathParts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                pathParts.add(part);
            }
        }

        if (pathParts.isEmpty()) {
            root.getLock().readLock().lock();
            try {
                return root;
            } finally {
                root.getLock().readLock().unlock();
            }
        }

        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        try {
            root.getLock().readLock().lock();
            lockedEntries.add(root);

            FileSystemEntry current = root;
            for (String part : pathParts) {
                if (!current.isDirectory()) {
                    return null;
                }
                Folder folder = (Folder) current;
                if (!folder.hasChildDirectly(part)) {
                    return null;
                }

                FileSystemEntry next = folder.getChildDirectly(part);
                next.getLock().readLock().lock();
                lockedEntries.add(next);
                current = next;
            }

            return current;
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    String createFolder(String path) {
        if (path.equals("/") || path.equals("/" + root.name)) {
            return "Error: Root already exists";
        }

        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        try {
            Folder parent = resolveParentWithLocks(path, lockedEntries);
            String folderName = extractFileName(path);

            parent.getLock().readLock().unlock();
            lockedEntries.remove(lockedEntries.size() - 1);

            parent.getLock().writeLock().lock();
            try {
                if (parent.hasChildDirectly(folderName)) {
                    return "Error: Entry already exists: " + folderName;
                }

                Folder folder = new Folder();
                folder.name = folderName;
                parent.addChildDirectly(folder);
                return "Created folder: " + folder.getPath();
            } finally {
                parent.getLock().writeLock().unlock();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    String delete(String path) {
        if (path.equals("/") || path.equals("/" + root.name)) {
            return "Error: Cannot delete root";
        }

        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        try {
            Folder parent = resolveParentWithLocks(path, lockedEntries);
            String name = extractFileName(path);

            parent.getLock().readLock().unlock();
            lockedEntries.remove(lockedEntries.size() - 1);

            parent.getLock().writeLock().lock();
            try {
                FileSystemEntry removed = parent.removeChildDirectly(name);
                if (removed == null) {
                    return "Error: Entry not found: " + path;
                }
                return "Deleted: " + path;
            } finally {
                parent.getLock().writeLock().unlock();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    String rename(String path, String newName) {
        if (path.equals("/") || path.equals("/" + root.name)) {
            return "Error: Cannot rename root";
        }

        if (newName == null || newName.isEmpty() || newName.contains("/")) {
            return "Error: Invalid name";
        }

        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        try {
            Folder parent = resolveParentWithLocks(path, lockedEntries);
            String oldName = extractFileName(path);

            parent.getLock().readLock().unlock();
            lockedEntries.remove(lockedEntries.size() - 1);

            parent.getLock().writeLock().lock();
            try {
                if (!parent.hasChildDirectly(oldName)) {
                    return "Error: Entry not found";
                }

                if (parent.hasChildDirectly(newName)) {
                    return "Error: Entry already exists: " + newName;
                }

                FileSystemEntry entry = parent.removeChildDirectly(oldName);
                entry.setName(newName);
                parent.addChildDirectly(entry);
                return "Renamed: " + oldName + " to " + newName;
            } finally {
                parent.getLock().writeLock().unlock();
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    String move(String srcPath, String destPath) {
        if (srcPath.equals("/") || srcPath.equals("/" + root.name)) {
            return "Error: Cannot move root";
        }

        List<FileSystemEntry> srcLockedEntries = new ArrayList<>();
        List<FileSystemEntry> destLockedEntries = new ArrayList<>();

        try {
            Folder srcParent = resolveParentWithLocks(srcPath, srcLockedEntries);
            String srcName = extractFileName(srcPath);

            srcParent.getLock().readLock().unlock();
            srcLockedEntries.remove(srcLockedEntries.size() - 1);
            srcParent.getLock().writeLock().lock();

            try {
                FileSystemEntry entry = srcParent.getChildDirectly(srcName);
                if (entry == null) {
                    return "Error: Source not found: " + srcPath;
                }

                entry.getLock().readLock().lock();

                try {
                    Folder destParent = resolveParentWithLocks(destPath, destLockedEntries);
                    String destName = extractFileName(destPath);

                    if (entry.isDirectory()) {
                        Folder current = destParent;
                        while (current != null) {
                            if (current == entry) {
                                return "Error: Cannot move folder into itself";
                            }
                            current = (Folder) current.getParent();
                        }
                    }

                    destParent.getLock().readLock().unlock();
                    destLockedEntries.remove(destLockedEntries.size() - 1);

                    if (srcParent == destParent) {
                        if (srcParent.hasChildDirectly(destName)) {
                            return "Error: Destination already exists: " + destPath;
                        }

                        FileSystemEntry movedEntry = srcParent.removeChildDirectly(srcName);
                        movedEntry.setName(destName);
                        srcParent.addChildDirectly(movedEntry);
                        return "Moved: " + srcPath + " to " + destPath;
                    }

                    int lockOrder = compareLockOrder(srcParent, destParent);
                    Folder first = lockOrder <= 0 ? srcParent : destParent;
                    Folder second = lockOrder <= 0 ? destParent : srcParent;

                    if (first != srcParent) {
                        srcParent.getLock().writeLock().unlock();
                    }

                    first.getLock().writeLock().lock();
                    second.getLock().writeLock().lock();

                    try {
                        if (destParent.hasChildDirectly(destName)) {
                            return "Error: Destination already exists: " + destPath;
                        }

                        FileSystemEntry movedEntry = srcParent.removeChildDirectly(srcName);
                        movedEntry.setName(destName);
                        destParent.addChildDirectly(movedEntry);
                        return "Moved: " + srcPath + " to " + destPath;
                    } finally {
                        second.getLock().writeLock().unlock();
                        first.getLock().writeLock().unlock();
                    }

                } finally {
                    entry.getLock().readLock().unlock();
                }

            } finally {
                if (((ReentrantReadWriteLock) srcParent.getLock()).isWriteLockedByCurrentThread()) {
                    srcParent.getLock().writeLock().unlock();
                }
            }

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            for (int i = srcLockedEntries.size() - 1; i >= 0; i--) {
                srcLockedEntries.get(i).getLock().readLock().unlock();
            }
            for (int i = destLockedEntries.size() - 1; i >= 0; i--) {
                destLockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    private int compareLockOrder(Folder f1, Folder f2) {
        return Integer.compare(System.identityHashCode(f1), System.identityHashCode(f2));
    }

    List<String> list(String path) {
        List<FileSystemEntry> lockedEntries = new ArrayList<>();

        try {
            FileSystemEntry entry = resolvePathWithLocks(path, lockedEntries);

            if (!entry.isDirectory()) {
                return Collections.singletonList("Error: Cannot list a file");
            }

            Folder folder = (Folder) entry;
            List<FileSystemEntry> children = new ArrayList<>(folder.children.values());
            List<String> result = new ArrayList<>();

            for (FileSystemEntry child : children) {
                result.add(child.name + (child.isDirectory() ? "/" : ""));
            }

            return result;

        } catch (Exception e) {
            return Collections.singletonList("Error: " + e.getMessage());
        } finally {
            for (int i = lockedEntries.size() - 1; i >= 0; i--) {
                lockedEntries.get(i).getLock().readLock().unlock();
            }
        }
    }

    private FileSystemEntry resolvePathWithLocks(String path, List<FileSystemEntry> lockedEntries) throws Exception {
        List<String> pathParts = parsePath(path);

        if (pathParts.isEmpty()) {
            root.getLock().readLock().lock();
            lockedEntries.add(root);
            return root;
        }

        if (!pathParts.get(0).equals(root.name)) {
            throw new Exception("Path must start with root: " + root.name);
        }

        root.getLock().readLock().lock();
        lockedEntries.add(root);

        Folder currentFolder = root;
        for (int i = 1; i < pathParts.size(); i++) {
            String name = pathParts.get(i);

            if (!currentFolder.hasChildDirectly(name)) {
                throw new Exception("Path not found: " + name);
            }

            FileSystemEntry entry = currentFolder.getChildDirectly(name);
            entry.getLock().readLock().lock();
            lockedEntries.add(entry);

            if (i < pathParts.size() - 1 && !entry.isDirectory()) {
                throw new Exception(name + " is not a directory");
            }

            if (entry.isDirectory()) {
                currentFolder = (Folder) entry;
            } else {
                return entry;
            }
        }

        return currentFolder;
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Testing Manual File System ===");
        // Create a file system structure: /root/documents/notes.txt
        Folder root = new Folder();
        root.name = "root";
        root.parent = null;

        Folder documents = new Folder();
        documents.name = "documents";
        documents.parent = root;

        File notes = new File();
        notes.name = "notes.txt";
        notes.parent = documents;

        // Test that getPath() works and the original objects are intact
        System.out.println("File path: " + notes.getPath());
        System.out.println("Folder path: " + documents.getPath());
        System.out.println("Root path: " + root.getPath());

        // Verify the original object is NOT modified
        System.out.println("\nVerifying original object is intact:");
        System.out.println("notes.name is still: " + notes.name);
        System.out.println("notes.parent is still: " + notes.parent.name);
        System.out.println("documents.parent is still: " + documents.parent.name);

        System.out.println("\n=== Testing FileSystem Class ===");
        FileSystem fs = new FileSystem();

        // First create folders
        Folder fsDocuments = new Folder();
        fsDocuments.name = "documents";
        fs.root.addChild(fsDocuments);

        Folder work = new Folder();
        work.name = "work";
        fsDocuments.addChild(work);

        Folder personal = new Folder();
        personal.name = "personal";
        fsDocuments.addChild(personal);

        Folder photos = new Folder();
        photos.name = "photos";
        fs.root.addChild(photos);

        // Create files (paths must start with /root/)
        System.out.println(fs.createFile("/root/documents/work/report.txt", "Q1 Report"));
        System.out.println(fs.createFile("/root/documents/personal/notes.txt", "My notes"));
        System.out.println(fs.createFile("/root/photos/vacation.jpg", "Beach photo"));

        // Try to create duplicate
        System.out.println(fs.createFile("/root/documents/work/report.txt", "Q2 Report"));

        // Test edge cases
        System.out.println(fs.createFile("/root", "content"));
        System.out.println(fs.createFile("/root/nonexistent/file.txt", "content"));

        System.out.println("\n=== Testing New Operations ===");

        System.out.println(fs.createFolder("/root/documents/archive"));
        System.out.println(fs.createFolder("/root/temp"));

        System.out.println("\nListing /root/documents:");
        for (String item : fs.list("/root/documents")) {
            System.out.println("  " + item);
        }

        System.out.println("\n" + fs.rename("/root/photos/vacation.jpg", "beach2023.jpg"));

        System.out.println(fs.move("/root/documents/work/report.txt", "/root/documents/archive/report.txt"));

        System.out.println("\nListing /root/documents/archive:");
        for (String item : fs.list("/root/documents/archive")) {
            System.out.println("  " + item);
        }

        System.out.println("\n" + fs.delete("/root/temp"));

        System.out.println("\n=== Testing Edge Cases ===");
        System.out.println(fs.rename("/root/documents/work/report.txt", "newname.txt"));
        System.out.println(fs.move("/root", "/root/other"));
        System.out.println(fs.move("/root/documents", "/root/documents/work/nested"));

        System.out.println("\n=== Finding files ===");
        FileSystemEntry found1 = fs.find("/root/documents/archive/report.txt");
        if (found1 != null) {
            System.out.println("Found: " + found1.getPath() + " (isDirectory: " + found1.isDirectory() + ")");
        }

        FileSystemEntry found2 = fs.find("/root/photos");
        if (found2 != null) {
            System.out.println("Found: " + found2.getPath() + " (isDirectory: " + found2.isDirectory() + ")");
            System.out.println("Contents:");
            for (String item : fs.list("/root/photos")) {
                System.out.println("  " + item);
            }
        }
    }
}

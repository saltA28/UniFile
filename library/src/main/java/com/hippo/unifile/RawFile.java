/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.unifile;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

class RawFile extends UniFile {
    private File mFile;

    RawFile(UniFile parent, File file) {
        super(parent);
        mFile = file;
    }

    @Override
    public UniFile createFile(String displayName) {
        final File target = new File(mFile, displayName);
        if (target.exists()) {
            if (target.isFile()) {
                return new RawFile(this, target);
            } else {
                return null;
            }
        } else {
            InputStream is = null;
            try {
                is = new FileInputStream(target);
                return new RawFile(this, target);
            } catch (IOException e) {
                Log.w(TAG, "Failed to createFile " + displayName + ": " + e);
                return null;
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    @Override
    public UniFile createDirectory(String displayName) {
        final File target = new File(mFile, displayName);
        if (target.isDirectory() || target.mkdir()) {
            return new RawFile(this, target);
        } else {
            return null;
        }
    }

    @Override
    @NonNull
    public Uri getUri() {
        return Uri.fromFile(mFile);
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public String getType() {
        if (mFile.isDirectory()) {
            return null;
        } else {
            return getTypeForName(mFile.getName());
        }
    }

    @Override
    public boolean isDirectory() {
        return mFile.isDirectory();
    }

    @Override
    public boolean isFile() {
        return mFile.isFile();
    }

    @Override
    public long lastModified() {
        return mFile.lastModified();
    }

    @Override
    public long length() {
        return mFile.length();
    }

    @Override
    public boolean canRead() {
        return mFile.canRead();
    }

    @Override
    public boolean canWrite() {
        return mFile.canWrite();
    }

    @Override
    public boolean ensureDir() {
        return mFile.isDirectory() || mFile.mkdirs();
    }

    @Override
    public boolean ensureFile() {
        if (!mFile.isFile()) {
            try {
                return mFile.createNewFile();
            } catch (IOException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public UniFile subFile(String displayName) {
        return new RawFile(this, new File(mFile, displayName));
    }

    @Override
    public boolean delete() {
        deleteContents(mFile);
        return mFile.delete();
    }

    @Override
    public boolean exists() {
        return mFile.exists();
    }

    @Override
    public UniFile[] listFiles() {
        final ArrayList<UniFile> results = new ArrayList<>();
        final File[] files = mFile.listFiles();
        if (files != null) {
            for (File file : files) {
                results.add(new RawFile(this, file));
            }
        }
        return results.toArray(new UniFile[results.size()]);
    }

    @Override
    public UniFile[] listFiles(FilenameFilter filter) {
        if (null == filter) {
            return listFiles();
        }

        final ArrayList<UniFile> results = new ArrayList<>();
        final File[] files = mFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (filter.accept(this, file.getName())) {
                    results.add(new RawFile(this, file));
                }
            }
        }
        return results.toArray(new UniFile[results.size()]);
    }

    @Override
    public UniFile findFile(String displayName) {
        final File child = new File(mFile, displayName);
        return child.exists() ? new RawFile(this, child) : null;
    }

    @Override
    public boolean renameTo(String displayName) {
        final File target = new File(mFile.getParentFile(), displayName);
        if (mFile.renameTo(target)) {
            mFile = target;
            return true;
        } else {
            return false;
        }
    }

    @Override
    @NonNull
    public OutputStream openOutputStream() throws IOException {
        return new FileOutputStream(mFile);
    }

    @Override
    @NonNull
    public OutputStream openOutputStream(boolean append) throws IOException {
        return new FileOutputStream(mFile, append);
    }

    @Override
    @NonNull
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(mFile);
    }

    @Override
    @NonNull
    public UniRandomReadFile createRandomReadFile() throws IOException {
        return new RawRandomReadFile(mFile);
    }

    private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }

        return "application/octet-stream";
    }

    private static boolean deleteContents(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContents(file);
                }
                if (!file.delete()) {
                    Log.w(TAG, "Failed to delete " + file);
                    success = false;
                }
            }
        }
        return success;
    }
}

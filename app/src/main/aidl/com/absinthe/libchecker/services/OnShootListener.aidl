// OnShootListener.aidl
package com.absinthe.libchecker.services;

// Declare any non-default types here with import statements

interface OnShootListener {
    void onShootFinished(long timestamp);
    void onProgressUpdated(int progress);
}
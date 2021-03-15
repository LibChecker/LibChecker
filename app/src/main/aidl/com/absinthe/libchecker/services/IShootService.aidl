// IShootService.aidl
package com.absinthe.libchecker.services;
import com.absinthe.libchecker.services.OnShootListener;

// Declare any non-default types here with import statements

interface IShootService {
    void computeSnapshot(boolean dropPrevious);
    void registerOnShootOverListener(in OnShootListener listener);
    void unregisterOnShootOverListener(in OnShootListener listener);
}
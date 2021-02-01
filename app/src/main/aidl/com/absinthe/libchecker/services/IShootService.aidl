// IShootService.aidl
package com.absinthe.libchecker.services;
import com.absinthe.libchecker.services.OnShootOverListener;

// Declare any non-default types here with import statements

interface IShootService {
    void computeSnapshot(boolean dropPrevious);
    void registerOnShootOverListener(in OnShootOverListener listener);
    void unregisterOnShootOverListener(in OnShootOverListener listener);
}
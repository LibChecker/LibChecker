// IShootService.aidl
package com.absinthe.libchecker.services;
import com.absinthe.libchecker.services.OnShootListener;

// Declare any non-default types here with import statements

interface IShootService {
    void computeSnapshot(in boolean dropPrevious);
    boolean isShooting();
    void registerOnShootOverListener(in OnShootListener listener);
    void unregisterOnShootOverListener(in OnShootListener listener);
}

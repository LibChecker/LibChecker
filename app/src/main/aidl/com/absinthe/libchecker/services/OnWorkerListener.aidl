package com.absinthe.libchecker.services;

interface OnWorkerListener {
    void onReceivePackagesChanged(in String packageName, in String action);
}

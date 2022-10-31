package ohos.app;

import java.io.File;

import ohos.aafwk.ability.IAbilityConnection;
import ohos.aafwk.ability.IAbilityStartCallback;
import ohos.aafwk.ability.startsetting.AbilityStartSetting;
import ohos.aafwk.content.Intent;
import ohos.app.dispatcher.TaskDispatcher;
import ohos.app.dispatcher.task.TaskPriority;
import ohos.bundle.AbilityInfo;
import ohos.bundle.ApplicationInfo;
import ohos.bundle.ElementName;
import ohos.bundle.HapModuleInfo;
import ohos.bundle.IBundleManager;
import ohos.global.configuration.Configuration;
import ohos.global.resource.ResourceManager;
import ohos.global.resource.solidxml.Pattern;
import ohos.global.resource.solidxml.Theme;
import ohos.utils.net.Uri;

public class ContextDeal implements Context {

    public ContextDeal(android.content.Context context, ClassLoader classLoader) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public TaskDispatcher createParallelTaskDispatcher(String s, TaskPriority taskPriority) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public TaskDispatcher createSerialTaskDispatcher(String s, TaskPriority taskPriority) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public TaskDispatcher getGlobalTaskDispatcher(TaskPriority taskPriority) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public TaskDispatcher getMainTaskDispatcher() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public TaskDispatcher getUITaskDispatcher() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ProcessInfo getProcessInfo() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public AbilityInfo getAbilityInfo() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ResourceManager getResourceManager() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getPreferencesDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getDatabaseDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getDistributedDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void switchToDeviceEncryptedStorageContext() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void switchToCredentialEncryptedStorageContext() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean isDeviceEncryptedStorage() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean isCredentialEncryptedStorage() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int verifyCallingPermission(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int verifySelfPermission(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int verifyCallingOrSelfPermission(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int verifyPermission(String s, int i, int i1) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ClassLoader getClassloader() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Context getApplicationContext() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void terminateAbility() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void terminateAbility(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void displayUnlockMissionMessage() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void lockMission() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void unlockMission() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getLocalClassName() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ElementName getElementName() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ElementName getCallingAbility() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getCallingBundle() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean stopAbility(Intent intent) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void startAbility(Intent intent, int i) {
        throw new RuntimeException("Stub!");
    }

    //<editor-fold desc="api level 7">
    @Override
    public void startAbility(Intent intent, AbilityStartSetting abilityStartSetting, IAbilityStartCallback iAbilityStartCallback) {
        throw new RuntimeException("Stub!");
    }
    //</editor-fold>

    @Override
    public void startAbility(Intent intent, int i, AbilityStartSetting abilityStartSetting) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void startAbilities(Intent[] intents) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Context createBundleContext(String s, int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean canRequestPermission(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void requestPermissionsFromUser(String[] strings, int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean connectAbility(Intent intent, IAbilityConnection iAbilityConnection) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void disconnectAbility(IAbilityConnection iAbilityConnection) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setDisplayOrientation(AbilityInfo.DisplayOrientation displayOrientation) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public IBundleManager getBundleManager() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getBundleName() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getBundleCodePath() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getBundleResourcePath() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getDataDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getCacheDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getCodeCacheDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File[] getExternalMediaDirs() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getNoBackupFilesDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getFilesDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getDir(String s, int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getExternalCacheDir() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File[] getExternalCacheDirs() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File getExternalFilesDir(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public File[] getExternalFilesDirs(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean deleteFile(String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public IAbilityManager getAbilityManager() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean terminateAbilityResult(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int getDisplayOrientation() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setShowOnLockScreen(boolean b) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setWakeUpScreen(boolean b) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void restart() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setTransitionAnimation(int i, int i1) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public boolean isUpdatingConfigurations() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setTheme(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Theme getTheme() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int getThemeId() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setPattern(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Pattern getPattern() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getAppType() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ResourceManager getResourceManager(Configuration configuration) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Object getLastStoredDataWhenConfigChanged() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void printDrawnCompleted() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyPermission(String s, String s1) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyUriPermission(Uri uri, int i, String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyCallerPermission(String s, String s1) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyCallerUriPermission(Uri uri, int i, String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyPermission(String s, int i, int i1, String s1) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyUriPermission(Uri uri, int i, int i1, int i2, String s) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void compelVerifyUriPermission(Uri uri, String s, String s1, int i, int i1, int i2, String s2) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int getColor(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getString(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getString(int i, Object... objects) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String[] getStringArray(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int[] getIntArray(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public String getProcessName() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Context getAbilityPackageContext() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public HapModuleInfo getHapModuleInfo() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public Uri getCaller() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void setColorMode(int i) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int getColorMode() {
        throw new RuntimeException("Stub!");
    }

    public void setApplication(Application application) {
        throw new RuntimeException("Stub!");
    }
}

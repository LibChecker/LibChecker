package ohos.app

import ohos.aafwk.ability.IAbilityConnection
import ohos.aafwk.ability.IAbilityStartCallback
import ohos.aafwk.ability.startsetting.AbilityStartSetting
import ohos.aafwk.content.Intent
import ohos.app.dispatcher.TaskDispatcher
import ohos.app.dispatcher.task.TaskPriority
import ohos.bundle.AbilityInfo
import ohos.bundle.ApplicationInfo
import ohos.bundle.ElementName
import ohos.bundle.HapModuleInfo
import ohos.bundle.IBundleManager
import ohos.global.configuration.Configuration
import ohos.global.resource.ResourceManager
import ohos.global.resource.solidxml.Pattern
import ohos.global.resource.solidxml.Theme
import ohos.utils.net.Uri
import java.io.File

class ContextDeal(context: android.content.Context, classLoader: ClassLoader) : Context {

    init {
        throw RuntimeException("Stub!")
    }

    override fun createParallelTaskDispatcher(s: String, taskPriority: TaskPriority): TaskDispatcher {
        throw RuntimeException("Stub!")
    }

    override fun createSerialTaskDispatcher(s: String, taskPriority: TaskPriority): TaskDispatcher {
        throw RuntimeException("Stub!")
    }

    override fun getGlobalTaskDispatcher(taskPriority: TaskPriority): TaskDispatcher {
        throw RuntimeException("Stub!")
    }

    override fun getMainTaskDispatcher(): TaskDispatcher {
        throw RuntimeException("Stub!")
    }

    override fun getUITaskDispatcher(): TaskDispatcher {
        throw RuntimeException("Stub!")
    }

    override fun getApplicationInfo(): ApplicationInfo {
        throw RuntimeException("Stub!")
    }

    override fun getProcessInfo(): ProcessInfo {
        throw RuntimeException("Stub!")
    }

    override fun getAbilityInfo(): AbilityInfo {
        throw RuntimeException("Stub!")
    }

    override fun getResourceManager(): ResourceManager {
        throw RuntimeException("Stub!")
    }

    override fun getResourceManager(configuration: Configuration): ResourceManager {
        throw RuntimeException("Stub!")
    }

    override fun getBundleName(): String {
        throw RuntimeException("Stub!")
    }

    override fun getBundleCodePath(): String {
        throw RuntimeException("Stub!")
    }

    override fun getBundleResourcePath(): String {
        throw RuntimeException("Stub!")
    }

    override fun getDataDir(): File {
        throw RuntimeException("Stub!")
    }

    override fun getCacheDir(): File {
        throw RuntimeException("Stub!")
    }

    override fun getCodeCacheDir(): File {
        throw RuntimeException("Stub!")
    }

    override fun getExternalMediaDirs(): Array<File> {
        throw RuntimeException("Stub!")
    }

    override fun getNoBackupFilesDir(): File {
        throw RuntimeException("Stub!")
    }

    override fun getFilesDir(): File {
        throw RuntimeException("Stub!")
    }

    override fun getDir(s: String, i: Int): File {
        throw RuntimeException("Stub!")
    }

    override fun getExternalCacheDir(): File {
        throw RuntimeException("Stub!")
    }

    override fun getExternalCacheDirs(): Array<File> {
        throw RuntimeException("Stub!")
    }

    override fun getExternalFilesDir(s: String): File {
        throw RuntimeException("Stub!")
    }

    override fun getExternalFilesDirs(s: String): Array<File> {
        throw RuntimeException("Stub!")
    }

    override fun deleteFile(s: String): Boolean {
        throw RuntimeException("Stub!")
    }

    override fun getAbilityManager(): IAbilityManager {
        throw RuntimeException("Stub!")
    }

    override fun terminateAbilityResult(i: Int): Boolean {
        throw RuntimeException("Stub!")
    }

    override fun getDisplayOrientation(): Int {
        throw RuntimeException("Stub!")
    }

    override fun setShowOnLockScreen(b: Boolean) {
        throw RuntimeException("Stub!")
    }

    override fun setWakeUpScreen(b: Boolean) {
        throw RuntimeException("Stub!")
    }

    override fun getLastStoredDataWhenConfigChanged(): Any {
        throw RuntimeException("Stub!")
    }

    override fun printDrawnCompleted() {
        throw RuntimeException("Stub!")
    }

    override fun compelVerifyPermission(s: String, s1: String) {
        throw RuntimeException("Stub!")
    }

    override fun compelVerifyUriPermission(uri: Uri, i: Int, s: String) {
        throw RuntimeException("Stub!")
    }

    override fun compelVerifyCallerPermission(s: String, s1: String) {
        throw RuntimeException("Stub!")
    }

    override fun compelVerifyCallerUriPermission(uri: Uri, i: Int, s: String) {
        throw RuntimeException("Stub!")
    }

    override fun compelVerifyPermission(s: String, i: Int, i1: Int, s1: String) {
        throw RuntimeException("Stub!")
    }

    override fun compelVerifyUriPermission(uri: Uri, i: Int, i1: Int, i2: Int, s: String) {
        throw RuntimeException("Stub!")
    }

    override fun verifySelfPermission(s: String): Int {
        throw RuntimeException("Stub!")
    }

    override fun verifyCallingPermission(s: String): Int {
        throw RuntimeException("Stub!")
    }

    override fun verifyCallingOrSelfPermission(s: String): Int {
        throw RuntimeException("Stub!")
    }

    override fun verifyPermission(s: String, i: Int, i1: Int): Int {
        throw RuntimeException("Stub!")
    }

    override fun getBundleManager(): IBundleManager {
        throw RuntimeException("Stub!")
    }

    override fun requestPermissionsFromUser(array: Array<String>, i: Int, permissionRequestResult: PermissionRequestResult) {
        throw RuntimeException("Stub!")
    }

    override fun canRequestPermission(s: String): Boolean {
        throw RuntimeException("Stub!")
    }

    override fun startAbility(intent: Intent, i: Int) {
        throw RuntimeException("Stub!")
    }

    override fun startAbility(intent: Intent, abilityStartSetting: AbilityStartSetting, i: Int) {
        throw RuntimeException("Stub!")
    }

    override fun startAbilityForResult(intent: Intent, i: Int, iAbilityStartCallback: IAbilityStartCallback) {
        throw RuntimeException("Stub!")
    }

    override fun startAbilityForResult(intent: Intent, abilityStartSetting: AbilityStartSetting, i: Int, iAbilityStartCallback: IAbilityStartCallback) {
        throw RuntimeException("Stub!")
    }

    override fun stopAbility(intent: Intent): Boolean {
        throw RuntimeException("Stub!")
    }

    override fun connectAbility(intent: Intent, iAbilityConnection: IAbilityConnection): Boolean {
        throw RuntimeException("Stub!")
    }

    override fun disconnectAbility(iAbilityConnection: IAbilityConnection) {
        throw RuntimeException("Stub!")
    }

    override fun getPattern(): Pattern {
        throw RuntimeException("Stub!")
    }

    override fun getTheme(): Theme {
        throw RuntimeException("Stub!")
    }

    override fun getTheme(resourceManager: ResourceManager): Theme {
        throw RuntimeException("Stub!")
    }

    override fun setPattern(pattern: Pattern) {
        throw RuntimeException("Stub!")
    }

    override fun setTheme(theme: Theme) {
        throw RuntimeException("Stub!")
    }

    override fun getHapModuleInfo(): HapModuleInfo {
        throw RuntimeException("Stub!")
    }

    override fun getElementName(): ElementName {
        throw RuntimeException("Stub!")
    }

    override fun getColorMode(): Int {
        throw RuntimeException("Stub!")
    }

    fun setApplication(application: Application) {
        throw RuntimeException("Stub!")
    }
}
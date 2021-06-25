import android.content.pm.PackageManager
import com.absinthe.libchecker.LibCheckerApp

object SystemServices {
    val packageManager: PackageManager by lazy { LibCheckerApp.context.packageManager }
}
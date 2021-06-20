package jonathanfinerty.once;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jonathanfinerty.once.Amount.moreThan;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Once {

    public static final int THIS_APP_INSTALL = 0;
    public static final int THIS_APP_VERSION = 1;
    public static final int THIS_APP_SESSION = 2;

    private static long lastAppUpdatedTime = -1;

    private static PersistedMap tagLastSeenMap;
    private static PersistedSet toDoSet;
    private static ArrayList<String> sessionList;

    private Once() {
    }

    /**
     * This method needs to be called before Once can be used.
     * Typically it will be called from your Application class's onCreate method.
     *
     * @param context Application context
     */
    public static void initialise(Context context) {
        tagLastSeenMap = new PersistedMap(context, "TagLastSeenMap");
        toDoSet = new PersistedSet(context, "ToDoSet");

        if (sessionList == null) {
            sessionList = new ArrayList<>();
        }

        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            lastAppUpdatedTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException ignored) {

        }
    }

    /**
     * Mark a tag as 'to do' within a given scope, if it has already marked to do or been done
     * within that scope then it will not be marked.
     *
     * @param scope The scope to not repeat the to do task in
     * @param tag   A string identifier unique to the operation.
     */
    public static void toDo(@Scope int scope, String tag) {

        List<Long> tagSeenList = tagLastSeenMap.get(tag);

        if (tagSeenList.isEmpty()) {
            toDoSet.put(tag);
            return;
        }

        Long tagLastSeen = tagSeenList.get(tagSeenList.size() - 1);

        if (scope == THIS_APP_VERSION && tagLastSeen <= lastAppUpdatedTime) {
            toDoSet.put(tag);
        }
    }

    /**
     * Mark a tag as 'to do' regardless of whether or not its ever been marked done before
     *
     * @param tag A string identifier unique to the operation.
     */
    public static void toDo(String tag) {
        toDoSet.put(tag);
    }

    /**
     * Checks if a tag is currently marked as 'to do'.
     *
     * @param tag A string identifier unique to the operation.
     * @return {@code true} if the operation associated with {@code tag} has been marked 'to do' and has not been passed to {@code markDone()} since.
     */
    public static boolean needToDo(String tag) {
        return toDoSet.contains(tag);
    }

    @Nullable
    public static Date lastDone(String tag) {
        List<Long> lastSeenTimeStamps = tagLastSeenMap.get(tag);
        if (lastSeenTimeStamps.isEmpty()) {
            return null;
        }
        long lastTimestamp = lastSeenTimeStamps.get(lastSeenTimeStamps.size() - 1);
        return new Date(lastTimestamp);
    }

    /**
     * Checks if a tag has been marked done, ever.
     * <p>
     * Equivalent of calling {@code beenDone(int scope, String tag)} with scope of {@code THIS_APP_INSTALL}.
     * </p>
     *
     * @param tag A string identifier unique to the operation.
     * @return {@code true} if the operation associated with {@code tag} has been marked done within
     * the given {@code scope}.
     */
    public static boolean beenDone(String tag) {
        return beenDone(THIS_APP_INSTALL, tag, moreThan(0));
    }

    /**
     * Check if a tag has been done a specific number of times.
     *
     * @param tag           A string identifier unique to the operation.
     * @param numberOfTimes Requirement for how many times the operation must have be done.
     * @return {@code true} if the operation associated with {@code tag} has been marked done the specific {@code numberOfTimes}.
     */
    public static boolean beenDone(String tag, CountChecker numberOfTimes) {
        return beenDone(THIS_APP_INSTALL, tag, numberOfTimes);
    }

    /**
     * Checks if a tag has been marked done, at least once, in a given scope.
     * <p>
     * Equivalent of calling {@code beenDone(int scope, String tag, CountChecker numberOfTimes)} with scope of {@code THIS_APP_INSTALL} and a numberOfTimes of {@code Amount.moreThan(0)}.
     * </p>
     *
     * @param scope The scope in which to check whether the tag has been done, either
     *              {@code THIS_APP_INSTALL} or {@code THIS_APP_VERSION}.
     * @param tag   A string identifier unique to the operation.
     * @return {@code true} if the operation associated with {@code tag} has been marked done within
     * the given {@code scope} at least once.
     */
    public static boolean beenDone(@Scope int scope, String tag) {
        return beenDone(scope, tag, moreThan(0));
    }

    /**
     * Checks if a tag has been marked done within a given scope a specific number of times.
     *
     * @param scope         The scope in which to check whether the tag has been done, either
     *                      {@code THIS_APP_INSTALL} or {@code THIS_APP_VERSION}.
     * @param tag           A string identifier unique to the operation.
     * @param numberOfTimes Requirement for how many times the operation must have be done.
     * @return {@code true} if the operation associated with {@code tag} has been marked done within
     * the given {@code scope} the specific {@code numberOfTimes}.
     */
    public static boolean beenDone(@Scope int scope, String tag, CountChecker numberOfTimes) {

        List<Long> tagSeenDates = tagLastSeenMap.get(tag);

        if (tagSeenDates.isEmpty()) {
            return false;
        }

        switch (scope) {
            case THIS_APP_INSTALL:
                return numberOfTimes.check(tagSeenDates.size());
            case THIS_APP_SESSION: {
                int counter = 0;
                for (String tagFromList : sessionList) {
                    if (tagFromList.equals(tag)) {
                        counter++;
                    }
                }
                return numberOfTimes.check(counter);
            }
            case THIS_APP_VERSION:
            default: {
                int counter = 0;
                for (Long seenDate : tagSeenDates) {
                    if (seenDate > lastAppUpdatedTime) {
                        counter++;
                    }
                }

                return numberOfTimes.check(counter);
            }
        }
    }

    /**
     * Checks if a tag has been marked done within a given time span at least once. (e.g. the last 5 minutes)
     *
     * @param timeUnit The units of time to work in.
     * @param amount   The quantity of timeUnit.
     * @param tag      A string identifier unique to the operation.
     * @return {@code true} if the operation associated with {@code tag} has been marked done
     * within the last provide time span.
     */
    public static boolean beenDone(TimeUnit timeUnit, long amount, String tag) {
        return beenDone(timeUnit, amount, tag, moreThan(0));
    }

    /**
     * Checks if a tag has been marked done within a given time span a specific number of times. (e.g. twice in the last 5 minutes)
     *
     * @param timeUnit      The units of time to work in.
     * @param amount        The quantity of timeUnit.
     * @param tag           A string identifier unique to the operation.
     * @param numberOfTimes Requirement for how many times the operation must have be done.
     * @return {@code true} if the operation associated with {@code tag} has been marked done at least {@code numberOfTimes}
     * within the last provide time span.
     */
    public static boolean beenDone(TimeUnit timeUnit, long amount, String tag, CountChecker numberOfTimes) {
        long timeInMillis = timeUnit.toMillis(amount);
        return beenDone(timeInMillis, tag, numberOfTimes);
    }

    /**
     * Checks if a tag has been marked done within a the last {@code timeSpanInMillis} milliseconds at least once.
     *
     * @param timeSpanInMillis How many milliseconds ago to check if a tag has been marked done
     *                         since.
     * @param tag              A string identifier unique to the operation.
     * @return {@code true} if the operation associated with {@code tag} has been marked done at least once
     * within the last X milliseconds.
     */
    public static boolean beenDone(long timeSpanInMillis, String tag) {
        return beenDone(timeSpanInMillis, tag, moreThan(0));
    }

    /**
     * Checks if a tag has been marked done within a the last {@code timeSpanInMillis} milliseconds
     * a specific number of times.
     *
     * @param timeSpanInMillis How many milliseconds ago to check if a tag has been marked done
     *                         since.
     * @param tag              A string identifier unique to the operation.
     * @param numberOfTimes    Requirement for how many times the operation must have be done.
     * @return {@code true} if the operation associated with {@code tag} has been marked done
     * within the last X milliseconds.
     */
    public static boolean beenDone(long timeSpanInMillis, String tag, CountChecker numberOfTimes) {
        List<Long> tagSeenDates = tagLastSeenMap.get(tag);

        if (tagSeenDates.isEmpty()) {
            return false;
        }

        int counter = 0;
        for (Long seenDate : tagSeenDates) {
            long sinceSinceCheckTime = new Date().getTime() - timeSpanInMillis;
            if (seenDate > sinceSinceCheckTime) {
                counter++;
            }
        }

        return numberOfTimes.check(counter);
    }

    /**
     * Marks a tag (associated with some operation) as done. The {@code tag} is marked done at the time
     * of calling this method.
     *
     * @param tag A string identifier unique to the operation.
     */
    public static void markDone(String tag) {
        tagLastSeenMap.put(tag, new Date().getTime());
        sessionList.add(tag);
        toDoSet.remove(tag);
    }

    /**
     * Clears a tag as done. All checks with {@code beenDone()} with that tag will return {@code false} until
     * it is marked done again.
     *
     * @param tag A string identifier unique to the operation.
     */
    public static void clearDone(String tag) {
        tagLastSeenMap.remove(tag);
        sessionList.remove(tag);
    }

    /**
     * Clears a tag as 'to do'. All checks with {@code needToDo()} with that tag will return {@code false} until
     * it is marked 'to do' again.
     *
     * @param tag A string identifier unique to the operation.
     */
    public static void clearToDo(String tag) {
        toDoSet.remove(tag);
    }

    /**
     * Clears all tags as done. All checks with {@code beenDone()} with any tag will return {@code false}
     * until they are marked done again.
     */
    public static void clearAll() {
        tagLastSeenMap.clear();
        sessionList.clear();
    }

    /**
     * Clears all tags as 'to do'. All checks with {@code needToDo()} with any tag will return {@code false}
     * until they are marked 'to do' again.
     */
    public static void clearAllToDos() {
        toDoSet.clear();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({THIS_APP_INSTALL, THIS_APP_VERSION, THIS_APP_SESSION})
    public @interface Scope {
    }
}

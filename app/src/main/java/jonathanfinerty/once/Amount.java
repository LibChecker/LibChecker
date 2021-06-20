package jonathanfinerty.once;

@SuppressWarnings("WeakerAccess")
public class Amount {

    public static CountChecker exactly(final int numberOfTimes) {
        return new CountChecker() {
            @Override
            public boolean check(int count) {
                return numberOfTimes == count;
            }
        };
    }

    public static CountChecker moreThan(final int numberOfTimes) {
        return new CountChecker() {
            @Override
            public boolean check(int count) {
                return count > numberOfTimes;
            }
        };
    }

    public static CountChecker lessThan(final int numberOfTimes) {
        return new CountChecker() {
            @Override
            public boolean check(int count) {
                return count < numberOfTimes;
            }
        };
    }
}

package io.smallrye.mutiny.helpers.test;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

public class AssertionHelper {

    static void shouldHaveCompleted(boolean completed, Throwable failure, List<?> items) {
        if (completed) {
            return;
        }
        if (failure != null) {
            fail("%nExpected a completion event, but received a failure event <%s>:%n<%s>",
                    failure, getStackTrace(failure));
        } else if (items != null && !items.isEmpty()) {
            fail("%nExpected a completion event, but didn't received it."
                    + "%nThe subscriber received item events:%n<%s>", getItemList(items));
        } else {
            fail("%nExpected a completion event, but didn't received it.");
        }
    }

    static void shouldHaveFailed(boolean completed, Throwable failure, Class<?> expectedFailureType,
            String expectedMessage) {
        if (completed) {
            fail("%nExpected a failure event, but instead got a completion event.");
        } else if (failure == null) {
            fail("%nExpected a failure event, but didn't received it. It didn't receive a completion event either.");
        }

        if (expectedFailureType != null && !(expectedFailureType.isInstance(failure))) {
            fail("%nReceived a failure event, but expecting:%n  <%s>%nto be an instance of:%n  <%s>%nbut was:%n  <%s>",
                    failure, expectedFailureType, getStackTrace(failure));
        }

        if (expectedMessage != null) {
            final String msg = failure.getMessage();
            if (msg == null) {
                fail("%nReceived a failure event, but expecting:%n  <%s>%nto contain:%n  <%s>%nbut was:%n  <%s>",
                        msg, expectedMessage, getStackTrace(failure));
            } else {
                if (!msg.contains(expectedMessage)) {
                    fail("%nReceived a failure event, but expecting:%n  <%s>%nto contain:%n  <%s>%nbut was:%n  <%s>",
                            msg, expectedMessage, getStackTrace(failure));
                }
            }
        }
    }

    static void shouldHaveReceivedNoItems(List<?> items) {
        if (!items.isEmpty()) {
            fail("%nExpected no item events but received %d items:%n<%s>", items.size(), getItemList(items));
        }
    }

    static void shouldBeSubscribed(int numberOfSubscriptions) {
        if (numberOfSubscriptions == 0) {
            fail("%nExpected having a subscription, but didn't received it.");
        }
        if (numberOfSubscriptions > 1) {
            fail("%nExpected a single subscription, but received %d subscriptions.", numberOfSubscriptions);
        }
    }

    static void shouldNotBeSubscribed(int numberOfSubscriptions) {
        if (numberOfSubscriptions != 0) {
            fail("%nExpected no subscription, but received %d subscriptions.", numberOfSubscriptions);
        }
    }

    static void shouldNotBeTerminated(boolean completed, Throwable failure) {
        if (completed) {
            fail("%nExpected no terminal event, but received a completion event.");
        } else if (failure != null) {
            fail("%nExpected no terminal event, but received a failure event: <%s>:%n<%s>",
                    failure, getStackTrace(failure));
        }
    }

    static void shouldNotBeTerminatedUni(boolean completed, Throwable failure) {
        if (completed) {
            fail("%nExpected no terminal event, but received a completion event.");
        } else if (failure != null && !(failure instanceof CancellationException)) {
            fail("%nExpected no terminal event, but received a failure event: <%s>:%n<%s>",
                    failure, getStackTrace(failure));
        }
    }

    static void shouldBeTerminated(boolean completed, Throwable failure) {
        if (!completed && failure == null) {
            fail("%nExpected a terminal event (either a completion or failure event), but didn't received any.");
        }
    }

    static void shouldHaveReceived(Object item, Object expected) {
        if (item != null && expected == null) {
            fail("%nExpected `null` but received <%s>", item);
        } else if (item == null && expected != null) {
            fail("%nExpected <%s> but received `null`", expected);
        } else if (item != null) {
            if (!item.equals(expected)) {
                fail("%nExpected <%s> but received <%s>", expected, item);
            }
        }
    }

    static void shouldHaveReceivedExactly(List<?> items, Object[] expected) {
        Map<Object, Object> missed = new LinkedHashMap<>();
        List<Object> extra = new ArrayList<>();

        Iterator<?> iterator = items.iterator();
        for (Object o : expected) {
            if (!iterator.hasNext()) {
                missed.put(o, null);
            } else {
                Object received = iterator.next();
                if (!received.equals(o)) {
                    missed.put(o, received);
                }
            }
        }

        while (iterator.hasNext()) {
            extra.add(iterator.next());
        }

        if (missed.isEmpty() && extra.isEmpty()) {
            // Everything is fine
            return;
        }

        if (!missed.isEmpty()) {
            fail("%nExpected to have received exactly:%n<%s>%nbut received:%n<%s>.%nMismatches are:%n%s",
                    getItemList(expected), getItemList(items), getMismatches(missed));
        }
        fail("%nExpected to have received exactly%n<%s>%nbut received%n<%s>.%nThe following items were not expected:%n<%s>",
                getItemList(expected), getItemList(items), getItemList(extra));
    }

    private static void fail(String msg, Object... params) {
        throw new AssertionError(String.format(msg, params));
    }

    private static String getItemList(List<?> items) {
        List<String> strings = items.stream().map(Object::toString).collect(Collectors.toList());
        return String.join(",", strings);
    }

    private static String getItemList(Object[] items) {
        List<String> strings = Arrays.stream(items).map(Object::toString).collect(Collectors.toList());
        return String.join(",", strings);
    }

    private static String getMismatches(Map<Object, Object> mismatches) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : mismatches.entrySet()) {
            if (builder.length() != 0) {
                builder.append("\n");
            }
            if (entry.getValue() == null) {
                builder.append("\t").append("- Missing expected item <").append(entry.getKey()).append(">");
            } else {
                builder.append("\t").append("- Expected <").append(entry.getValue()).append("> to be equal to <")
                        .append(entry.getKey()).append(">");
            }
        }
        return builder.toString();
    }

    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = null;
        PrintWriter pw = null;

        String result;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw, true);
            throwable.printStackTrace(pw);
            result = sw.getBuffer().toString();
        } finally {
            closeQuietly(sw);
            closeQuietly(pw);
        }

        return result;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // Ignored.
            }
        }
    }

}
